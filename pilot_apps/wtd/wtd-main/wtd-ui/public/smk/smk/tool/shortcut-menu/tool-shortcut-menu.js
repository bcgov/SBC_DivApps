include.module( 'tool-shortcut-menu', [ 'tool', 'tool-shortcut-menu.shortcut-menu-html' ], function ( inc ) {
    "use strict";

    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    function ShortcutMenuTool( option ) {    
        SMK.TYPE.Tool.prototype.constructor.call( this, $.extend( {
            order: 10
        }, option ) )

        this.model = {
            widgets: []
        }
    }

    SMK.TYPE.ShortcutMenuTool = ShortcutMenuTool

    $.extend( ShortcutMenuTool.prototype, SMK.TYPE.Tool.prototype )
    ShortcutMenuTool.prototype.afterInitialize = []
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    ShortcutMenuTool.prototype.afterInitialize.push( function ( smk ) {
        var self = this

        this.vm = new Vue( {
            el: smk.addToStatus( inc[ 'tool-shortcut-menu.shortcut-menu-html' ] ),
            data: this.model,
            methods: {
                trigger: function ( toolId, event, arg ) {
                    smk.emit( toolId, event, arg )
                }
            }
        } )
    } )
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    ShortcutMenuTool.prototype.addTool = function ( tool, smk ) {
        var self = this

        this.model.widgets.push( {
            id:                 tool.id,
            widgetComponent:    tool.widgetComponent,
            widget:             tool.widget,
        } )

        tool.useList = false
        smk.$tool[ 'list-menu' ].addTool( tool )
    }

    return ShortcutMenuTool
} )

