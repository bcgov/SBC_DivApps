<template>
  <v-container fluid>
    <v-card>
      <v-card-title v-if="canEdit">
        <v-row>
          <v-col>
            <v-btn id="edit-btn" max-width="40" :disabled="isEditing" @click="editAction">
              Edit
            </v-btn>
          </v-col>
          <v-col v-if="isEditing">
            <v-btn id="add-tab-btn" class="first-text-input" @click="addTabAction">
              <v-icon>mdi-shape-square-plus</v-icon>
              Add Tab
            </v-btn>
            <v-btn id="add-tile-btn" @click="addTileAction">
              <v-icon>mdi-playlist-plus</v-icon>
              Add Tile
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
                </v-row>
              </v-card-text>
            </v-card>
            <v-card
              v-for="(tile, index) in dashboard.tiles"
              :key="index"
              elevation="2"
              outlined
            >
              <v-card-title :class="{ hide: isEditing }">{{tile.tileName}}</v-card-title>
              <v-card-text v-if="tile.tileType=='SSRS_LINK' && !isEditing">
                <iframe
                  frameBorder="0"
                  scrolling="no"
                  :style="getTileHight(tile.tileHeight)"
                  :src="tile.tileURL">
                </iframe>
              </v-card-text>
              <v-card-text v-if="tile.tileType=='WAIT_MAP' && !isEditing">
                <wait-time-map>
                </wait-time-map>
              </v-card-text>
              <v-card-text v-if="isEditing" class="edit-row">
                <v-row no-gutters>
                  <v-col
                    cols="1"
                  >
                    <v-btn id="delete-tile-btn" @click="deleteTileAction(index)">
                      <v-icon>mdi-delete-forever</v-icon>
                    </v-btn>
                  </v-col>
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
                    cols="9"
                    sm="3"
                    md="5"
                  >
                    <v-text-field
                      class="first-text-input"
                      label="Tile Title"
                      v-model="tile.tileName"
                    />
                  </v-col>
                  <v-col
                    cols="1"
                  >
                    <v-text-field
                      class="first-text-input"
                      label="Height"
                      v-model="tile.tileHeight"
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
                      @blur="handleBlurURL($event)"
                      class="first-text-input"
                      label="Tile URL"
                      v-model="tile.tileURL"
                    />
                  </v-col>
                  <v-col
                    cols="6"
                    md="4"
                  >
                    <v-combobox
                      v-model="tile.tileGroups"
                      :items="keyCloakGroups"
                      :search-input.sync="search"
                      hide-selected
                      label="Security Groups"
                      multiple
                      persistent-hint
                      small-chips
                      @change="search=''"
                    >
                      <template v-slot:no-data>
                        <v-list-item>
                          <v-list-item-content>
                            <v-list-item-title>
                              Press <kbd>enter</kbd> to add this group
                            </v-list-item-title>
                          </v-list-item-content>
                        </v-list-item>
                      </template>
                    </v-combobox>
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
import { SearchResponseIF, DashboardTabIF, DashboardTileIF } from '@/interfaces' // eslint-disable-line no-unused-vars

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

  @Prop({ default: null })
  private keyCloakGroups: string[]

  tabNumber: number = 0

  @Prop({ default: 'https://gov.bc.ca' })
  private registryUrl: string

  @Prop({ default: null })
  private dashboards: DashboardTabIF[]

  visibleDashboards: DashboardTabIF[] = []

  private search: string = ''

  private isEditing: boolean = false

  private getTileHight (height: string): string {
    if (height === '') {
      return 'width:100%;height:200px;'
    }
    return 'width:100%;height:' + height + 'px;'
  }

  private tileTypeList = [
    { key: APITileTypes.SSRS, desc: UITileTypes.SSRS },
    { key: APITileTypes.WAIT_MAP, desc: UITileTypes.WAIT_MAP }
  ]

  private get isAuthenticated (): boolean {
    return Boolean(sessionStorage.getItem(SessionStorageKeys.KeyCloakToken))
  }

  /** Redirects browser to Business Registry home page. */
  private redirectRegistryHome (): void {
    window.location.assign(this.registryUrl)
  }

  private handleBlurURL (e: FocusEvent) {
    const val: string = (e.target as HTMLInputElement).value.toLowerCase()
    if (!val.startsWith('http://') && !val.startsWith('https://')) {
      (e.target as HTMLInputElement).value = 'http://' + val
      e.target.dispatchEvent(new Event('input'))
    }
  }

  /** Called when App is ready and this component can load its data. */
  @Watch('appReady')
  private async onAppReady (val: boolean): Promise<void> {
    console.log('app ready!')
    // do not proceed if app is not ready
    if (!val) return
    this.visibleDashboards = []
    this.dashboards.forEach(val => this.visibleDashboards.push(Object.assign({}, val)))

    this.emitHaveData()

    if (this.keyCloakGroups != null) {
      this.canEdit = this.keyCloakGroups.indexOf(sessionStorage.getItem('EDIT_GROUP')) >= 0
    }
  }

  /** Emits Have Data event. */
  @Emit('haveData')
  private emitHaveData (haveData: Boolean = true): void { }

  @Emit('updateDashboards')
  private emitUpdateDashboard (dashboards: DashboardTabIF[]): void { }

  @Emit('getUpdateDashboards')
  private emitGetUpdateDashboards (): void { }

  private editAction () {
    this.isEditing = true
    this.emitGetUpdateDashboards()
  }

  private canEdit: boolean = false;

  private saveAction () {
    // this.isEditing = false
    // this.isEditing = !this.isEditing
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
    // this.visibleDashboards = []
    // this.dashboards.forEach(val => this.visibleDashboards.push(Object.assign({}, val)))
    this.isEditing = false
    location.reload()
  }

  private addTileAction () {
    const newTile: DashboardTileIF = {
      tileName: 'New Tile',
      tileOrder: 99,
      tileType: 'SSRS_LINK',
      tileURL: '',
      tileGroups: [],
      tileHeight: 300
    }
    this.visibleDashboards[this.tabNumber].tiles.push(newTile)
  }

  private addTabAction () {
    const newTab: DashboardTabIF = {
      tabOrder: 99,
      tabName: 'New Tab',
      tiles: []
    }
    this.visibleDashboards.push(newTab)
  }

  private deleteTileAction (index: number) {
    this.visibleDashboards[this.tabNumber].tiles.splice(index, 1)
  }

  private deleteTab (index: number) {
    this.visibleDashboards.splice(index, 1)
  }

  created () {
    // this.$cookies.set('wtd-rp', sessionStorage.getItem(SessionStorageKeys.KeyCloakToken))
    this.$cookies.set(
      'wtd-rp',
      sessionStorage.getItem(SessionStorageKeys.KeyCloakToken),
      null,
      null,
      'apps.silver.devops.gov.bc.ca') // domain address
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

.edit-row {
  margin-top: 15px;
}

.hide {
  display: none;
}
</style>
