include.module( 'tool-search-location', [ 'tool', 'widgets', 'tool-search-location.panel-search-location-html', 'tool-search-location.address-html' ], function ( inc ) {
    "use strict";

    Vue.component( 'address-display', {
        template: inc[ 'tool-search-location.address-html' ],
        props: [ 'address' ]
    } )

    Vue.component( 'search-location-panel', {
        extends: inc.widgets.toolPanel,
        template: inc[ 'tool-search-location.panel-search-location-html' ],
        props: [ 'feature', 'tool' ]
    } )

    function SearchLocationTool( option ) {
        this.makeProp( 'feature', {} )
        this.makeProp( 'tool', {} )

        SMK.TYPE.Tool.prototype.constructor.call( this, $.extend( {
            panelComponent: 'search-location-panel',
            titleComponent: 'address-display'
        }, option ) )
    }


    SMK.TYPE.SearchLocationTool = SearchLocationTool

    $.extend( SearchLocationTool.prototype, SMK.TYPE.Tool.prototype )
    SearchLocationTool.prototype.afterInitialize = []
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    SearchLocationTool.prototype.afterInitialize.push( function ( smk ) {
        var self = this

        self.changedActive( function () {
            if ( self.active ) {
                smk.$tool[ 'search' ].visible = true
                smk.$viewer.searched.highlight()
            }
            else {
                smk.$tool[ 'search' ].visible = false
            }
        } )

        if ( smk.$tool.directions )
            this.tool.directions = true

        smk.on( this.id, {
            'directions': function () {
                smk.$tool.directions.active = true

                smk.$tool.directions.activating
                    .then( function () {
                        return smk.$tool.directions.startAtCurrentLocation()
                    } )
                    .then( function () {
                        return SMK.UTIL.findNearestSite( { latitude: self.feature.geometry.coordinates[ 1 ], longitude: self.feature.geometry.coordinates[ 0 ] } )
                            .then( function ( site ) {
                                return smk.$tool.directions.addWaypoint( site )
                            } )
                            .catch( function ( err ) {
                                console.warn( err )
                                return smk.$tool.directions.addWaypoint()
                            } )
                    } )
            }
        } )

        smk.$viewer.searched.pickedFeature( function ( ev ) {
            self.feature = ev.feature

            if ( ev.feature ) {
                self.title = { address: ev.feature.properties }
                self.active = true
            }
        } )

    } )

    return SearchLocationTool
} )
