package com.pos.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pos.client.data.model.Floor
import com.pos.client.data.model.TableRepository
import com.pos.client.data.model.TableStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TableListViewModel(
    private val repository: TableRepository // 依存性の注入 (DI)
) : ViewModel() {

    // 1. 画面の状態を管理する StateFlow を定義

    // 全フロアの情報
    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors.asStateFlow()

    // 全テーブルの情報 (APIから取得するデータ)
    private val _allTables = MutableStateFlow<List<TableStatus>>(emptyList())

    // 現在選択されているフロアID (初期値は 1Fを想定)
    private val _selectedFloorId = MutableStateFlow(1)
    val selectedFloorId: StateFlow<Int> = _selectedFloorId.asStateFlow()

    // データのロード状態 (ローディング表示などに使用)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 選択されたフロアにフィルタリングされたテーブルのリスト
    val currentFloorTables: StateFlow<List<TableStatus>> = _allTables
        .combine(_selectedFloorId) { allTables, selectedId ->
            allTables.filter { it.floorId == selectedId }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 初期化ブロック
    init {
        // ViewModel生成時にデータをロード
        // ★★★ 修正箇所：フロア取得も非同期（launch）で行う ★★★
        viewModelScope.launch {
            _isLoading.value = true // ローディング開始
            _floors.value = repository.getFloors() // suspend関数を呼び出す

            // フロア取得後に最初のフロアを選択するロジックをここに移動
            if (_floors.value.isNotEmpty()) {
                _selectedFloorId.value = _floors.value.first().floorId
            }
            // テーブルステータスも取得（フロア取得後）
            fetchTableStatuses()
        }
    }

    // 2. APIからテーブルステータスを取得する関数
    fun fetchTableStatuses() {
        // Coroutineを開始し、非同期でI/O処理を行う
        viewModelScope.launch {
            _isLoading.value = true // ローディング開始
            try {
                // Repository経由でAPIを呼び出し
                val statuses = repository.getTableStatuses()
                _allTables.value = statuses // 取得したデータを状態に反映

            } catch (e: Exception) {
                println("テーブルステータス取得エラー: ${e.message}")
            } finally {
                _isLoading.value = false // ローディング終了
            }
        }
    }

    // 3. フロア選択を切り替える関数
    fun selectFloor(floorId: Int) {
        _selectedFloorId.value = floorId
    }
}