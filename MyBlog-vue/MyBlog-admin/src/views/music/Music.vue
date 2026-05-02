<template>
  <el-card class="main-card">
    <div class="title">{{ this.$route.name }}</div>
    <div class="operation-container">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="openModel(null)"> 新增 </el-button>
      <el-button
        type="danger"
        size="small"
        icon="el-icon-delete"
        :disabled="musicIdList.length == 0"
        @click="deleteFlag = true">
        批量删除
      </el-button>
      <div class="filter-container">
        <el-select v-model="status" size="small" clearable placeholder="状态" style="width: 110px">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="关闭" />
        </el-select>
        <el-input
          v-model="keywords"
          prefix-icon="el-icon-search"
          size="small"
          placeholder="请输入歌曲名或歌手"
          style="width: 220px"
          @keyup.enter.native="searchMusics" />
        <el-button type="primary" size="small" icon="el-icon-search" @click="searchMusics"> 搜索 </el-button>
      </div>
    </div>
    <el-table border :data="musicList" v-loading="loading" @selection-change="selectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column prop="cover" label="封面" width="90" align="center">
        <template slot-scope="scope">
          <el-image
            v-if="scope.row.cover"
            :src="scope.row.cover"
            style="width: 42px; height: 42px; border-radius: 6px"
            fit="cover"
            :preview-src-list="[scope.row.cover]" />
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column prop="musicName" label="歌曲名" min-width="160" align="center" show-overflow-tooltip />
      <el-table-column prop="artist" label="歌手" min-width="140" align="center" show-overflow-tooltip />
      <el-table-column prop="album" label="专辑" min-width="140" align="center" show-overflow-tooltip />
      <el-table-column prop="theme" label="主题色" width="100" align="center">
        <template slot-scope="scope">
          <span class="theme-dot" :style="{ backgroundColor: scope.row.theme || '#409EFF' }" />
          {{ scope.row.theme || '#409EFF' }}
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" align="center" />
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status == 1 ? 'success' : 'info'">
            {{ scope.row.status == 1 ? '启用' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" align="center">
        <template slot-scope="scope">
          <i class="el-icon-time" style="margin-right: 5px" />
          {{ scope.row.createTime | date }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="openModel(scope.row)"> 编辑 </el-button>
          <el-popconfirm title="确定删除吗？" style="margin-left: 1rem" @confirm="deleteMusic(scope.row.id)">
            <el-button size="mini" type="danger" slot="reference"> 删除 </el-button>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pagination-container"
      background
      @size-change="sizeChange"
      @current-change="currentChange"
      :current-page="current"
      :page-size="size"
      :total="count"
      :page-sizes="[10, 20]"
      layout="total, sizes, prev, pager, next, jumper" />

    <el-dialog :visible.sync="deleteFlag" width="30%">
      <div class="dialog-title-container" slot="title"><i class="el-icon-warning" style="color: #ff9900" />提示</div>
      <div style="font-size: 1rem">是否删除选中项？</div>
      <div slot="footer">
        <el-button @click="deleteFlag = false">取消</el-button>
        <el-button type="primary" @click="deleteMusic(null)"> 确定</el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="addOrEdit" width="42%" top="8vh">
      <div class="dialog-title-container" slot="title" ref="musicTitle" />
      <el-form label-width="90px" size="medium" :model="musicForm">
        <el-form-item label="歌曲名">
          <el-input v-model="musicForm.musicName" />
        </el-form-item>
        <el-form-item label="歌手">
          <el-input v-model="musicForm.artist" />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="musicForm.album" />
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="musicForm.cover" />
        </el-form-item>
        <el-form-item label="音频地址">
          <el-input v-model="musicForm.url" />
        </el-form-item>
        <el-form-item label="歌词地址">
          <el-input v-model="musicForm.lrc" />
        </el-form-item>
        <el-form-item label="主题色">
          <div class="theme-picker">
            <el-color-picker v-model="musicForm.theme" />
            <el-input v-model="musicForm.theme" style="width: 180px; margin-left: 12px" />
          </div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="musicForm.sort" controls-position="right" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="musicForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="musicForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="addOrEdit = false">取消</el-button>
        <el-button type="primary" @click="saveOrUpdateMusic"> 确定</el-button>
      </div>
    </el-dialog>
  </el-card>
</template>

<script>
export default {
  created() {
    this.current = this.$store.state.pageState.music
    this.listMusics()
  },
  data() {
    return {
      loading: true,
      deleteFlag: false,
      addOrEdit: false,
      keywords: null,
      status: null,
      musicIdList: [],
      musicList: [],
      musicForm: {
        id: null,
        musicName: '',
        artist: '',
        album: '',
        cover: '',
        url: '',
        lrc: '',
        theme: '#409EFF',
        sort: 1,
        status: 1,
        remark: ''
      },
      current: 1,
      size: 10,
      count: 0
    }
  },
  methods: {
    selectionChange(musicList) {
      this.musicIdList = []
      musicList.forEach((item) => {
        this.musicIdList.push(item.id)
      })
    },
    searchMusics() {
      this.current = 1
      this.listMusics()
    },
    sizeChange(size) {
      this.size = size
      this.listMusics()
    },
    currentChange(current) {
      this.current = current
      this.$store.commit('updateMusicPageState', current)
      this.listMusics()
    },
    listMusics() {
      this.axios
        .get('/api/admin/musics', {
          params: {
            current: this.current,
            size: this.size,
            keywords: this.keywords,
            status: this.status
          }
        })
        .then(({ data }) => {
          this.musicList = data.data.records
          this.count = data.data.count
          this.loading = false
        })
    },
    openModel(music) {
      if (music != null) {
        this.musicForm = JSON.parse(JSON.stringify(music))
        this.musicForm.theme = this.musicForm.theme || '#409EFF'
        this.$refs.musicTitle.innerHTML = '修改音乐'
      } else {
        this.musicForm = {
          id: null,
          musicName: '',
          artist: '',
          album: '',
          cover: '',
          url: '',
          lrc: '',
          theme: '#409EFF',
          sort: 1,
          status: 1,
          remark: ''
        }
        this.$refs.musicTitle.innerHTML = '新增音乐'
      }
      this.addOrEdit = true
    },
    saveOrUpdateMusic() {
      if (this.musicForm.musicName.trim() == '') {
        this.$message.error('歌曲名不能为空')
        return false
      }
      if (this.musicForm.artist.trim() == '') {
        this.$message.error('歌手不能为空')
        return false
      }
      if (this.musicForm.url.trim() == '') {
        this.$message.error('音频地址不能为空')
        return false
      }
      this.axios.post('/api/admin/musics', this.musicForm).then(({ data }) => {
        if (data.flag) {
          this.$notify.success({
            title: '成功',
            message: data.message
          })
          this.listMusics()
        } else {
          this.$notify.error({
            title: '失败',
            message: data.message
          })
        }
        this.addOrEdit = false
      })
    },
    deleteMusic(id) {
      let param = {}
      if (id == null) {
        param = { data: this.musicIdList }
      } else {
        param = { data: [id] }
      }
      this.axios.delete('/api/admin/musics', param).then(({ data }) => {
        if (data.flag) {
          this.$notify.success({
            title: '成功',
            message: data.message
          })
          this.listMusics()
        } else {
          this.$notify.error({
            title: '失败',
            message: data.message
          })
        }
        this.deleteFlag = false
      })
    }
  }
}
</script>

<style scoped>
.filter-container {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
.theme-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
}
.theme-picker {
  display: flex;
  align-items: center;
}
</style>
