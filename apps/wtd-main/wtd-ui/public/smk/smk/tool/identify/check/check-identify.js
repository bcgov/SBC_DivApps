include.module( 'check-identify', [], function ( inc ) {
    "use strict";

    return function ( smk, tool ) {
        var showFeatures 

        if ( !tool.showFeatures || tool.showFeatures == 'auto' ) {
            if ( smk.$device == 'desktop' ) {
                showFeatures = 'identify-popup'
            }
            else {
                showFeatures = 'identify-feature'
            }
        }
        else {
            showFeatures = tool.showFeatures
        }
        
        tool.showFeatures = showFeatures

        smk.tools.push( {
            type: showFeatures,
            enabled: true,
            position: tool.position
        } )
    }
    
} )
