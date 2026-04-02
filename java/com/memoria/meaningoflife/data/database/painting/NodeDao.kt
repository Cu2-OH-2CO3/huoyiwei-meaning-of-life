package com.memoria.meaningoflife.data.database.painting

import androidx.room.*

@Dao
interface NodeDao {

    @Query("SELECT * FROM nodes WHERE work_id = :workId ORDER BY node_order ASC")
    fun getNodesByWorkId(workId: Long): List<NodeEntity>

    @Query("SELECT * FROM nodes WHERE id = :nodeId")
    fun getNodeById(nodeId: Long): NodeEntity?

    @Insert
    fun insertNode(node: NodeEntity): Long

    @Update
    fun updateNode(node: NodeEntity)

    @Delete
    fun deleteNode(node: NodeEntity)

    @Query("SELECT * FROM nodes")
    fun getAllNodesSync(): List<NodeEntity>

    @Query("DELETE FROM nodes")
    fun deleteAll()

    @Query("DELETE FROM nodes")
    fun deleteAllSync()

    @Query("DELETE FROM nodes WHERE work_id = :workId")
    fun deleteNodesByWorkId(workId: Long)

    @Query("SELECT MAX(node_order) FROM nodes WHERE work_id = :workId")
    fun getMaxNodeOrder(workId: Long): Int?
}