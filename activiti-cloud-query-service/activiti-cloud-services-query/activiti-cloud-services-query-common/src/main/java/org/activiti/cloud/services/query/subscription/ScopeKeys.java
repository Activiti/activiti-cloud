/*
 * Copyright 2017-2026 Hyland Software, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.cloud.services.query.subscription;

import java.util.Objects;

/**
 * Internal addressing for pushed counts: one {@code <type>:<userId>} key per count type per user.
 * Clients never send these — the consumer produces them and the query-rest relay matches them, so
 * both sides must use this class as the single source of construction, parsing and ownership
 * checks. The prefixes defined here are reserved and must not collide with other key shapes.
 */
public final class ScopeKeys {

    /** The three pushed count types, each with the reserved prefix used in its scope key. */
    public enum PushedCountType {
        ASSIGNED("assigned"),
        QUEUED("queued"),
        PROCESSES("processes");

        private final String prefix;

        PushedCountType(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        static PushedCountType fromPrefix(String prefix) {
            for (PushedCountType type : values()) {
                if (type.prefix.equals(prefix)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown pushed count type prefix: " + prefix);
        }
    }

    /** Parsed form of a scope key. */
    public record ScopeKey(PushedCountType type, String userId) {}

    private static final String SEPARATOR = ":";

    private ScopeKeys() {}

    public static String of(PushedCountType type, String userId) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(userId, "userId");
        return type.prefix() + SEPARATOR + userId;
    }

    public static String assigned(String userId) {
        return of(PushedCountType.ASSIGNED, userId);
    }

    public static String queued(String userId) {
        return of(PushedCountType.QUEUED, userId);
    }

    public static String processes(String userId) {
        return of(PushedCountType.PROCESSES, userId);
    }

    /**
     * Parses a scope key. Splits on the first separator only, so a {@code userId} that itself
     * contains {@value #SEPARATOR} is preserved intact.
     */
    public static ScopeKey parse(String scopeKey) {
        Objects.requireNonNull(scopeKey, "scopeKey");
        int separatorAt = scopeKey.indexOf(SEPARATOR);
        if (separatorAt < 0) {
            throw new IllegalArgumentException("Not a scope key: " + scopeKey);
        }
        PushedCountType type = PushedCountType.fromPrefix(scopeKey.substring(0, separatorAt));
        String userId = scopeKey.substring(separatorAt + SEPARATOR.length());
        if (userId.isEmpty()) {
            throw new IllegalArgumentException("Scope key has no userId: " + scopeKey);
        }
        return new ScopeKey(type, userId);
    }

    /** Answers "does this key belong to this user id?" — the check the relay uses to route a count. */
    public static boolean belongsTo(String scopeKey, String userId) {
        Objects.requireNonNull(userId, "userId");
        return parse(scopeKey).userId().equals(userId);
    }
}
