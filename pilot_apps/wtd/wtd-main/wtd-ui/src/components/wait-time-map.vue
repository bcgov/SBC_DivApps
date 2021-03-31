<template>
  <v-container fluid no-gutters>
    <v-row no-gutters>
      <div class="wait-map" id="smk-map-frame"></div>
    </v-row>
  </v-container>
</template>
<script lang="ts">
// external
import { Component, Vue } from 'vue-property-decorator'

@Component({})
export default class WaitTimeMap extends Vue {
  mounted () {
    console.log('WaitTimeMap mounted')
    const mapUrl: string = sessionStorage.getItem('WTD_API_URL') + '/map/config'
    const mapPlugin = document.createElement('script')
    // rlo - todo move this to the proper place
    mapPlugin.setAttribute(
      'src',
      'smk/smk.js'
    )
    mapPlugin.setAttribute(
      'smk-container-sel',
      '#smk-map-frame'
    )
    mapPlugin.setAttribute(
      'smk-config',
      mapUrl
    )
    // mapPlugin.setAttribute(
    //   'smk-base-url',
    //   'wtd'
    // )
    // mapPlugin.setAttribute(
    //   'smk-id',
    //   'my-map'
    // )
    mapPlugin.async = true
    document.head.appendChild(mapPlugin)
  }
}

</script>

<style lang="scss" scoped>
@import "@/assets/styles/theme.scss";

.wait-map {
  width: 100%;
  height: 500px;
  position: static;
}
</style>
