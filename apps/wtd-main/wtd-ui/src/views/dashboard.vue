<template>
  <v-container fluid>
    <v-card>
      <v-card-title v-if="canEdit">
        <v-row>
          <v-col>
            <v-btn id="edit-btn" max-width="40" @click="editAction">
              Edit
            </v-btn>
          </v-col>
          <v-col v-if="isEditing">
            <v-layout align-end justify-end>
              <v-btn id="cancel-btn" max-width="50" @click="cancelAction">
                Cancel
              </v-btn>
              <v-btn id="save-btn" max-width="50" @click="saveAction">
                Save
              </v-btn>
            </v-layout>
          </v-col>
        </v-row>
      </v-card-title>
      <v-tabs
        v-model="tabNumber"
        background-color="primary"
        dark
        fixed-tabs
      >
        <v-tabs-slider color="yellow"></v-tabs-slider>
        <v-tab
          v-for="(n, index) in visibleDashboards"
          :key="index"
        >
          {{ n.tabName }}
          <v-icon v-if="isEditing" right @click="deleteTab(index)">mdi-delete</v-icon>
        </v-tab>
        <v-tabs-items v-model="tabNumber">
          <v-tab-item
            v-for="(dashboard, index) in visibleDashboards"
            :key="index"
          >
            <v-card v-if="isEditing"
              elevation="2"
              outlined
              class="tab-edit"
            >
              <v-card-text>
                <v-row no-gutters>
                  <v-col
                    cols="1"
                  >
                    <v-text-field
                      label="Tab Order"
                      class="first-text-input"
                      v-model="dashboard.tabOrder"
                    />
                  </v-col>
                  <v-col
                    cols="11"
                    sm="5"
                    md="7"
                  >
                    <v-text-field
                      label="Tab Title"
                      class="first-text-input"
                      v-model="dashboard.tabName"
                    />
                  </v-col>
                  <v-col
                    cols="6"
                    md="4"
                  >
                    <v-select
                      :items="roleList"
                      label="Roles"
                      outlined
                    ></v-select>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
            <v-card
              v-for="(tile, index) in dashboard.tiles"
              :key="index"
              elevation="2"
              outlined
            >
              <v-card-title v-if="!isEditing">{{tile.tileName}}</v-card-title>
              <v-card-text v-if="!isEditing && tile.tileType=='SSRS_LINK'">
                <iframe frameBorder="0" scrolling="no" style="width:100%;height:300px" :src="tile.tileURL"></iframe>
              </v-card-text>
              <v-card-text v-else-if="!isEditing && tile.tileType=='WAIT_MAP'">
                <wait-time-map>
                </wait-time-map>
              </v-card-text>
              <v-card-text v-if="isEditing">
                <v-row no-gutters>
                  <v-col
                    cols="1"
                  >
                    <v-text-field
                      label="Tile Order"
                      class="first-text-input"
                      v-model="tile.tileOrder"
                    />
                  </v-col>
                  <v-col
                    cols="11"
                    sm="5"
                    md="7"
                  >
                    <v-text-field
                      class="first-text-input"
                      label="Tile Title"
                      v-model="tile.tileName"
                    />
                  </v-col>
                  <v-col
                    cols="6"
                    md="4"
                  >
                    <v-select
                      :items="tileTypeList"
                      item-text="desc"
                      item-value="key"
                      label="Tile Type"
                      v-model="tile.tileType"
                      outlined
                    ></v-select>
                  </v-col>
                </v-row>
                <v-row no-gutters>
                  <v-col
                    cols="12"
                    sm="6"
                    md="8"
                  >
                    <v-text-field
                      @blur="handleBlueURL"
                      class="first-text-input"
                      label="Tile URL"
                      v-model="tile.tileURL"
                    />
                  </v-col>
                  <v-col
                    cols="6"
                    md="4"
                  >
                    <v-select
                      :items="roleList"
                      label="Roles"
                      outlined
                    ></v-select>
                  </v-col>
                </v-row>
              </v-card-text>
            </v-card>
          </v-tab-item>
        </v-tabs-items>
      </v-tabs>
    </v-card>
  </v-container>
</template>

<script lang="ts">
// external
import { Component, Emit, Prop, Vue, Watch } from 'vue-property-decorator'
// bcregistry
import { SessionStorageKeys } from 'sbc-common-components/src/util/constants'

// local
// import { getFeatureFlag } from '@/utils'
import { WaitTimeMap } from '@/components'
import { SearchResponseIF, DashboardTabIF } from '@/interfaces' // eslint-disable-line no-unused-vars

import { UITileTypes, APITileTypes } from '@/enums'

@Component({
  components: {
    WaitTimeMap
  }
})
export default class Dashboard extends Vue {
  /** Whether App is ready. */
  @Prop({ default: false })
  private appReady: boolean

  tabNumber: number = 0

  @Prop({ default: 'https://bcregistry.ca' })
  private registryUrl: string

  @Prop({ default: null })
  private dashboards: DashboardTabIF[]

  visibleDashboards: DashboardTabIF[] = []

  private isEditing: boolean = false

  private canEdit: boolean = true

  private tileTypeList = [
    { key: APITileTypes.SSRS, desc: UITileTypes.SSRS },
    { key: APITileTypes.WAIT_MAP, desc: UITileTypes.WAIT_MAP }
  ]

  private roleList: string[] = [
    'ROLE1', 'ROLE2', 'ADMIN'
  ]

  private get isAuthenticated (): boolean {
    return Boolean(sessionStorage.getItem(SessionStorageKeys.KeyCloakToken))
  }

  /** Redirects browser to Business Registry home page. */
  private redirectRegistryHome (): void {
    window.location.assign(this.registryUrl)
  }

  private handleBlueURL (e: FocusEvent) {
    // if (!e.target.value.toLowerCase().startsWith('http://')) {
    //   e.target.value = 'http://' + e.target.value
    //   e.target.dispatchEvent(new Event('input'))
    // }
  }

  /** Called when App is ready and this component can load its data. */
  @Watch('appReady')
  private async onAppReady (val: boolean): Promise<void> {
    console.log('app ready!')
    // do not proceed if app is not ready
    if (!val) return

    this.dashboards.forEach(val => this.visibleDashboards.push(Object.assign({}, val)))

    this.emitHaveData()
  }

  /** Emits Have Data event. */
  @Emit('haveData')
  private emitHaveData (haveData: Boolean = true): void { }

  @Emit('updateDashboards')
  private emitUpdateDashboard (dashboards: DashboardTabIF[]): void { }

  private editAction () {
    this.isEditing = !this.isEditing
  }

  private saveAction () {
    this.isEditing = !this.isEditing
    /** Sort the tabs based on tabOrder */
    this.visibleDashboards.sort((a, b) => a.tabOrder - b.tabOrder)
    /** Renumber the tabOrder in case the tabOrder is not in sequential order */
    let counter: number = 1
    for (var i in this.visibleDashboards) {
      this.visibleDashboards[i].tabOrder = counter++
      /** sort each of the tile order as well */
      this.visibleDashboards[i].tiles.sort((a, b) => a.tileOrder - b.tileOrder)
      let tileCounter: number = 1
      /** Renumber the tileOrder in case the tileOrder is not in sequential order */
      for (var j in this.visibleDashboards[i].tiles) {
        this.visibleDashboards[i].tiles[j].tileOrder = tileCounter++
      }
    }
    this.emitUpdateDashboard(this.visibleDashboards)
  }

  private cancelAction () {
    this.visibleDashboards = []
    this.dashboards.forEach(val => this.visibleDashboards.push(Object.assign({}, val)))
    this.isEditing = false
  }

  private deleteTab (index: number) {
    console.log('delete tab ' + index)
    this.visibleDashboards.splice(index, 1)
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/theme.scss';
.search-title {
  font-size: 1rem;
  color: $gray9;
}
.first-text-input {
  margin-right: 26px;
}
#save-btn {
  margin-left: 16px;
}
iframe {
  overflow: hidden;
}
.tab-edit {
  background-color: rgb(187, 209, 243);
}
</style>
