/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.tests.integration.dbclient.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;

/**
 * Common testing code.
 */
public abstract class AbstractIT {

    private static final Logger LOG = Logger.getLogger(AbstractIT.class.getName());

    public static final Config CONFIG = Config.create(ConfigSources.classpath("test.yaml"));

    public static final DbClient dbClient = initDbClient();

    public static DbClient initDbClient() {
        Config dbConfig = CONFIG.get("db");
        return DbClient.builder(dbConfig).build();
    }

    /**
     * Pokemon type POJO.
     */
    public static final class Type {
        private final int id;
        private final String name;

        public Type(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Pokemon POJO.
     */
    public static final class Pokemon {

        private final int id;
        private final String name;
        private final List<Type> types;

        public Pokemon(int id, String name, Type... types) {
            this.id = id;
            this.name = name;
            this.types = new ArrayList<>(types != null ? types.length : 0);
            for (Type type : types) {
               this.types.add(type);
            }
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<Type> getTypes() {
            return types;
        }

        public Type[] getTypesArray() {
            return types.toArray(new Type[types.size()]);
        }

    }

    /** Map of pokemon types. */
    public static final Map<Integer, Type> TYPES = new HashMap<>();

    // Initialize pokemon types Map
    static {
        TYPES.put(1, new Type(1, "Normal"));
        TYPES.put(2, new Type(2, "Fighting"));
        TYPES.put(3, new Type(3, "Flying"));
        TYPES.put(4, new Type(4, "Poison"));
        TYPES.put(5, new Type(5, "Ground"));
        TYPES.put(6, new Type(6, "Rock"));
        TYPES.put(7, new Type(7, "Bug"));
        TYPES.put(8, new Type(8, "Ghost"));
        TYPES.put(9, new Type(9, "Steel"));
        TYPES.put(10, new Type(10, "Fire"));
        TYPES.put(11, new Type(11, "Water"));
        TYPES.put(12, new Type(12, "Grass"));
        TYPES.put(13, new Type(13, "Electric"));
        TYPES.put(14, new Type(14, "Psychic"));
        TYPES.put(15, new Type(15, "Ice"));
        TYPES.put(16, new Type(16, "Dragon"));
        TYPES.put(17, new Type(17, "Dark"));
        TYPES.put(18, new Type(18, "Fairy"));
    }

    /** Map of pokemons. */
    public static final Map<Integer, Pokemon> POKEMONS = new HashMap<>();

    // Initialize pokemons Map
    static {
        // Pokemons for query tests
        POKEMONS.put(1, new Pokemon(1, "Pikachu", TYPES.get(13)));
        POKEMONS.put(2, new Pokemon(2, "Raichu", TYPES.get(13)));
        POKEMONS.put(3, new Pokemon(3, "Machop", TYPES.get(2)));
        POKEMONS.put(4, new Pokemon(4, "Snorlax", TYPES.get(1)));
        POKEMONS.put(5, new Pokemon(5, "Charizard", TYPES.get(10), TYPES.get(3)));
    }

    /** Last used id in Pokemons table. */
    public static final int LAST_POKEMON_ID = 5;
}