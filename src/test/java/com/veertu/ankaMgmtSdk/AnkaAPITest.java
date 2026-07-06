package com.veertu.ankaMgmtSdk;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Root-cause coverage for the over-provisioning / orphaned-VM regression (issue #76).
 *
 * <p>{@link AnkaAPI#showInstance} used to answer purely from a 7s-cached {@code GET /api/v1/vm}
 * snapshot. A VM that {@code startVM()} had just created could be missing from a snapshot taken
 * before it existed, so {@code showInstance} returned a false {@code null}; provisioning then gave
 * up, the durable node lingered as an orphan, and a replacement was spun up (over-provisioning).
 *
 * <p>The fix makes {@code showInstance} cache-aware: on a miss for a known id it forces one
 * authoritative refresh before concluding the VM is gone, and coalesces concurrent misses so a
 * burst does not stampede the controller.
 */
public class AnkaAPITest {

    private static final String VM_ID = "vm-1";

    private static AnkaVmInstance instance(String id) {
        JSONObject json = new JSONObject();
        json.put("instance_state", "Started");
        json.put("vmid", "tmpl-" + id);
        return new AnkaVmInstance(id, json);
    }

    @Test
    public void showInstanceForcesFreshListWhenCachedSnapshotMissesKnownId() throws Exception {
        AnkaMgmtCommunicator communicator = mock(AnkaMgmtCommunicator.class);
        // The TTL refresh sees the pre-VM (empty) snapshot; the forced refresh-on-miss then sees
        // the just-started VM.
        when(communicator.list())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(instance(VM_ID)));
        AnkaAPI api = new AnkaAPI(communicator);

        AnkaVmInstance found = api.showInstance(VM_ID);

        assertThat("A known VM missing from the cached snapshot must be found via a forced refresh",
                found, is(notNullValue()));
        assertThat(found.id, is(VM_ID));
        verify(communicator, times(2)).list(); // one TTL refresh + one forced refresh-on-miss
    }

    @Test
    public void showInstanceReturnsNullWhenTrulyAbsentAndForcesOnlyOneExtraList() throws Exception {
        AnkaMgmtCommunicator communicator = mock(AnkaMgmtCommunicator.class);
        when(communicator.list()).thenReturn(Collections.emptyList());
        AnkaAPI api = new AnkaAPI(communicator);
        // Pre-fresh the cache so getNewData() does not list; isolates the single forced refresh.
        api.cacheInstances(Collections.emptyList());

        AnkaVmInstance found = api.showInstance("does-not-exist");

        assertThat("A genuinely absent VM returns null after one authoritative refresh",
                found, is(nullValue()));
        verify(communicator, times(1)).list(); // exactly one forced refresh, no herd
    }

    @Test
    public void concurrentMissesOnSameSnapshotCoalesceToASingleForcedRefresh() throws Exception {
        final int threads = 24;
        AnkaMgmtCommunicator communicator = mock(AnkaMgmtCommunicator.class);
        // First list (the TTL refresh) is the pre-VM empty snapshot; every later list returns the VM.
        when(communicator.list())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(instance(VM_ID)));
        final AnkaAPI api = new AnkaAPI(communicator);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CyclicBarrier startAll = new CyclicBarrier(threads);
        List<Future<AnkaVmInstance>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startAll.await();
                return api.showInstance(VM_ID);
            }));
        }
        try {
            for (Future<AnkaVmInstance> future : futures) {
                AnkaVmInstance result = future.get(30, TimeUnit.SECONDS);
                assertThat("Every concurrent caller resolves the VM", result, is(notNullValue()));
                assertThat(result.id, is(VM_ID));
            }
        } finally {
            pool.shutdownNow();
        }

        // One TTL refresh + exactly one forced refresh serve every caller: once the forced list
        // populates the VM the losers skip (the snapshot version has moved) and the cache hit covers
        // the rest, so the controller is never stampeded.
        verify(communicator, times(2)).list();
    }
}
