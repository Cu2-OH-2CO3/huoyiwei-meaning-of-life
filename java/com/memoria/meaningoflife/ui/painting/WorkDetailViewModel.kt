package com.memoria.meaningoflife.ui.painting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.meaningoflife.MeaningOfLifeApp
import com.memoria.meaningoflife.data.database.painting.NodeEntity
import com.memoria.meaningoflife.data.database.painting.WorkEntity
import com.memoria.meaningoflife.data.repository.PaintingRepository
import kotlinx.coroutines.launch

class WorkDetailViewModel : ViewModel() {

    private val repository = PaintingRepository(MeaningOfLifeApp.instance.database)

    private val _work = MutableLiveData<WorkEntity?>()
    val work: LiveData<WorkEntity?> = _work

    private val _nodes = MutableLiveData<List<NodeEntity>>()
    val nodes: LiveData<List<NodeEntity>> = _nodes

    fun loadWork(workId: Long) {
        viewModelScope.launch {
            val work = repository.getWorkById(workId)
            _work.postValue(work)

            if (work != null) {
                val nodes = repository.getNodesByWorkId(workId)
                _nodes.postValue(nodes)
            }
        }
    }

    fun deleteNode(node: NodeEntity) {
        viewModelScope.launch {
            repository.deleteNode(node)
            // 重新加载节点
            _work.value?.let { work ->
                val nodes = repository.getNodesByWorkId(work.id)
                _nodes.postValue(nodes)

                // 更新作品总时长
                val totalDuration = nodes.filter { it.id != node.id }.sumOf { it.duration }
                val updatedWork = work.copy(totalDuration = totalDuration)
                repository.updateWork(updatedWork)
                _work.postValue(updatedWork)
            }
        }
    }

    fun deleteWork() {
        viewModelScope.launch {
            _work.value?.let { work ->
                repository.deleteWork(work.id)
            }
        }
    }
}