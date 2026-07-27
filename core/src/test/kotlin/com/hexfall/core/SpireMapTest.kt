package com.hexfall.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpireMapTest {

    @Test
    fun `every generated map is fully traversable to the boss`() {
        repeat(50) { seed ->
            val map = SpireMap.generate(Random(seed.toLong()))
            val boss = map.node(map.bossId)
            assertEquals(NodeType.BOSS, boss.type)

            // BFS from every starting node must reach the boss.
            for (start in map.available(null)) {
                var frontier = setOf(start.id)
                val seen = mutableSetOf<Int>()
                while (frontier.isNotEmpty()) {
                    seen += frontier
                    frontier = frontier
                        .flatMap { map.node(it).next }
                        .filterNot { it in seen }
                        .toSet()
                }
                assertTrue(map.bossId in seen, "seed $seed: start ${start.id} cannot reach boss")
            }
        }
    }

    @Test
    fun `every non-boss node has at least one outgoing edge`() {
        repeat(50) { seed ->
            val map = SpireMap.generate(Random(seed.toLong()))
            map.nodes.filter { it.id != map.bossId }.forEach { node ->
                assertTrue(node.next.isNotEmpty(), "seed $seed: node ${node.id} is a dead end")
            }
        }
    }

    @Test
    fun `structural placement rules hold`() {
        repeat(50) { seed ->
            val map = SpireMap.generate(Random(seed.toLong()))
            map.nodes.forEach { node ->
                when (node.row) {
                    0 -> assertEquals(NodeType.MONSTER, node.type)
                    7 -> assertEquals(NodeType.TREASURE, node.type)
                    SpireMap.ROWS - 1 -> assertEquals(NodeType.REST, node.type)
                }
                if (node.type == NodeType.ELITE) assertTrue(node.row >= 4)
            }
        }
    }

    @Test
    fun `same seed generates the same map`() {
        val a = SpireMap.generate(Random(123))
        val b = SpireMap.generate(Random(123))
        assertEquals(
            a.nodes.map { Triple(it.id, it.row to it.col, it.type) },
            b.nodes.map { Triple(it.id, it.row to it.col, it.type) },
        )
    }
}
