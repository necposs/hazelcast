package com.github.necposs.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;

/**
 * Minimal CP Subsystem example: starts a 3-member cluster in one JVM
 * and increments a shared {@link IAtomicLong}.
 */
public final class CpSample {

    private static final int CP_MEMBER_COUNT = 3;

    private CpSample() {
    }

    public static void main(String[] args) {
        HazelcastInstance[] members = new HazelcastInstance[CP_MEMBER_COUNT];
        try {
            for (int i = 0; i < members.length; i++) {
                Config config = new Config();
                config.getCPSubsystemConfig().setCPMemberCount(CP_MEMBER_COUNT);
                members[i] = Hazelcast.newHazelcastInstance(config);
            }

            IAtomicLong counter = members[0].getCPSubsystem().getAtomicLong("counter");
            counter.set(10);
            long incremented = counter.incrementAndGet();
            boolean swapped = counter.compareAndSet(incremented, 100);

            // Same CP object seen from another member
            IAtomicLong sameCounter = members[1].getCPSubsystem().getAtomicLong("counter");
            System.out.println("incremented=" + incremented
                    + ", swapped=" + swapped
                    + ", value seen by member 2=" + sameCounter.get());
        } finally {
            Hazelcast.shutdownAll();
        }
    }
}
