include.module( 'widgets', [ 'vue', 'widgets.tool-button-html' ], function ( inc ) {
    "use strict";

    var emit = {
        methods: {
            $$emit: function ( event, arg ) {
                this.$root.trigger( this.id, event, arg )
            }
        }
    }

    return {
        emit: emit,

        toolButton: Vue.extend( {
            mixins: [ emit ],
            template: inc[ 'widgets.tool-button-html' ],
            props: { 'id': String, 'type': String, 'title': String, 'visible': Boolean, 'enabled': Boolean, 'active': Boolean, 'icon': String, 'showTitle': Boolean },
            computed: {
                classes: function () {
                    var c = {}
                    c[ 'smk-tool' ] = true
                    c[ 'smk-' + this.type + '-tool' ] = true
                    return Object.assign( c, {
                        'smk-tool-active': this.active,
                        'smk-tool-visible': this.visible,
                        'smk-tool-enabled': this.enabled,
                        'smk-tool-title': this.showTitle
                    } )
                }
            }
        } ),

        toolPanel: Vue.extend( {
            mixins: [ emit ],
            props: [ 'id', 'title', 'visible', 'enabled', 'active' ],
        } )

    }

} )