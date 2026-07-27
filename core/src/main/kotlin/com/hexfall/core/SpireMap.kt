package com.hexfall.core

import kotlin.random.Random

enum class NodeType { MONSTER, ELITE, EVENT, REST, SHOP, TREASURE, BOSS }

/**
 * A node in the act map. [row] 0 is the bottom; the boss sits alone on the
 * top row. [col] positions the node horizontally for drawing.
 */
data class MapNode(
    val id: Int,
    val row: Int,
    val col: Int,
    val type: NodeType,
    val next: MutableSet<Int> = mutableSetOf(),
)

class SpireMap(val nodes: List<MapNode>, val bossId: Int) {

    fun node(id: Int): MapNode = nodes.first { it.id == id }

    /** Nodes reachable from [currentId]; the bottom row when the run starts. */
    fun available(currentId: Int?): List<MapNode> =
        if (currentId == null) {
            nodes.filter { it.row == 0 }
        } else {
            node(currentId).next.map { node(it) }
        }

    companion object {
        const val ROWS = 15
        const val COLS = 7
        private const val WALKS = 5

        /**
         * Slay-the-Spire-style layered map: several random walks from bottom
         * to top are merged into a DAG, then node types are assigned with
         * placement rules (treasure mid-act, rest before the boss, no
         * elites/rests near the start).
         */
        fun generate(rng: Random): SpireMap {
            val grid = Array(ROWS) { arrayOfNulls<MapNode>(COLS) }
            var nextId = 0
            val edges = mutableSetOf<Pair<Int, Int>>()

            repeat(WALKS) {
                var col = rng.nextInt(COLS)
                var prev: MapNode? = null
                for (row in 0 until ROWS) {
                    val node = grid[row][col] ?: MapNode(nextId++, row, col, NodeType.MONSTER)
                        .also { grid[row][col] = it }
                    prev?.let { edges += it.id to node.id }
                    prev = node
                    col = (col + rng.nextInt(-1, 2)).coerceIn(0, COLS - 1)
                }
            }

            val nodes = grid.flatMap { row -> row.filterNotNull() }.toMutableList()
            val byId = nodes.associateBy { it.id }
            edges.forEach { (from, to) -> byId.getValue(from).next += to }

            assignTypes(nodes, rng)

            val boss = MapNode(nextId, ROWS, COLS / 2, NodeType.BOSS)
            nodes.filter { it.row == ROWS - 1 }.forEach { it.next += boss.id }
            nodes += boss
            return SpireMap(nodes, boss.id)
        }

        private fun assignTypes(nodes: MutableList<MapNode>, rng: Random) {
            fun replace(node: MapNode, type: NodeType) {
                // copy() shares the mutable `next` set, which is what we want:
                // edges survive the type change.
                nodes[nodes.indexOf(node)] = node.copy(type = type)
            }

            for (node in nodes.toList()) {
                val type = when {
                    node.row == 0 -> NodeType.MONSTER
                    node.row == 7 -> NodeType.TREASURE
                    node.row == ROWS - 1 -> NodeType.REST
                    else -> {
                        val roll = rng.nextInt(100)
                        when {
                            roll < 14 && node.row >= 4 -> NodeType.ELITE
                            roll < 27 -> NodeType.EVENT
                            roll < 37 && node.row >= 5 -> NodeType.REST
                            roll < 47 && node.row >= 2 -> NodeType.SHOP
                            else -> NodeType.MONSTER
                        }
                    }
                }
                if (type != node.type) replace(node, type)
            }
        }
    }
}
