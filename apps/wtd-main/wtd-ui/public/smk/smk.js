// SMK
if ( !window.include ) { ( function () {
    "use strict";

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    var TAG = {}
    var OPTION = {
        baseUrl: document.location,
        timeout: 60 * 1000
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    function includeTag( tag, attr ) {
        if ( !attr ) {
            if ( !TAG[ tag ] ) throw new Error( 'tag "' + tag + '" not defined' )

            return TAG[ tag ]
        }

        if ( tag in TAG )
            throw new Error( 'tag "' + tag + '" already defined' )

        TAG[ tag ] = attr
        return attr
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    function option( option ) {
        if ( typeof option == 'string' ) return OPTION[ option ]
        Object.assign( OPTION, option )
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    var loader = {}

    loader.$resolveUrl = function ( url ) {
        if ( /^[.][/]/.test( url ) ) return url

        return ( new URL( url, OPTION.baseUrl ) ).toString()
    }

    loader.tags = function ( inc ) {
        return this.template( inc )
            .then( function ( data ) {
                var tagData = JSON.parse( data )
                var tags = Object.keys( tagData )
                tags.forEach( function ( t ) {
                    includeTag( t, tagData[ t ] )
                } )
                return tagData
            } )
    }

    loader.script = function ( inc ) {
        var self = this

        if ( inc.load ) {
            return new Promise( function ( res, rej ) {
                res( inc.load.call( window ) )
            } )
        }
        else if ( inc.url ) {
            return new Promise( function ( res, rej ) {
                var script = document.createElement( 'script' )

                if ( inc.integrity ) {
                    script.setAttribute( 'integrity', inc.integrity )
                    script.setAttribute( 'crossorigin', '' )
                }

                script.addEventListener( 'load', function( ev ) {
                    res( script )
                } )

                script.addEventListener( 'error', function( ev ) {
                    rej( new Error( 'failed to load script from ' + script.src ) )
                } )

                script.setAttribute( 'src', self.$resolveUrl( inc.url ) )

                document.getElementsByTagName( 'head' )[ 0 ].appendChild( script );
            } )
        }
        else throw new Error( 'Can\'t load script' )
    }

    loader.style = function ( inc ) {
        var self = this

        return new Promise( function ( res, rej ) {
            var style
            if ( inc.load ) {
                style = document.createElement( 'style' )
                style.textContent = inc.load
                res( style )
            }
            else {
                style = document.createElement( 'link' )

                style.setAttribute( 'rel', 'stylesheet' )

                if ( inc.integrity ) {
                    style.setAttribute( 'integrity', inc.integrity )
                    style.setAttribute( 'crossorigin', '' )
                }

                style.addEventListener( 'load', function( ev ) {
                    res( style )
                } )

                style.addEventListener( 'error', function( ev ) {
                    rej( new Error( 'failed to load stylesheet from ' + style.href ) )
                } )

                if ( inc.url ) {
                    style.setAttribute( 'href', self.$resolveUrl( inc.url ) )
                }
                else {
                    rej( new Error( 'Can\'t load style' ) )
                }
            }

            document.getElementsByTagName( 'head' )[ 0 ].appendChild( style );
        } )
    }

    loader.template = function ( inc ) {
        var self = this

        if ( inc.load ) {
            return new Promise( function ( res, rej ) {
                res( inc.data = inc.load )
            } )
        }
        else if ( inc.url ) {
            return new Promise( function ( res, rej ) {
                var req = new XMLHttpRequest()
                var url = self.$resolveUrl( inc.url )

                req.addEventListener( 'load', function () {
                    if ( this.status != 200 ) rej( new Error( 'status ' + this.status + ' trying to load template from ' + url ) )
                    res( inc.data = this.responseText )
                } )

                req.addEventListener( 'error', function ( ev ) {
                    rej( new Error( 'failed to load template from ' + url ) )
                } )

                req.open( 'GET', url )
                req.send()
            } )
        }
        else throw new Error( 'Can\'t load template' )
    }

    loader.sequence = function ( inc, tag ) {
        inc.tags.forEach( function ( t, i, a ) {
            a[ i ] = _assignAnonTag( t, tag )
        } )

        // console.group( tag, 'sequence', JSON.stringify( inc.tags ) )

        var promise = Promise.resolve()
        var res = {}

        inc.tags.forEach( function ( t ) {
            promise = promise.then( function () {
                return _include( t )
            } )
            .then( function ( r ) {
                res[ t ] = r
            } )
        } )

        return promise.then( function () {
            // console.groupEnd()
            return res
        } )
    }

    loader.group = function ( inc, tag ) {
        inc.tags.forEach( function ( t, i, a ) {
            a[ i ] = _assignAnonTag( t, tag )
        } )

        // console.group( tag, 'group', JSON.stringify( inc.tags ) )

        var promises = inc.tags.map( function ( t ) {
            return Promise.resolve().then( function () { return _include( t ) } )
        } )

        return Promise.all( promises )
            .then( function ( ress ) {
                // console.groupEnd()
                var res = {}
                inc.tags.forEach( function ( t, i ) {
                    res[ t ] = ress[ i ]
                } )
                return res
            } )
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    function include( tags ) {
        if ( !Array.isArray( tags ) )
            tags = [].slice.call( arguments )

        // return loader.group( { tags: tags } )
        return loader.sequence( { tags: tags } )
    }

    var extLoader = {
        js: 'script',
        css: 'style',
        html: 'template'
    }

    function _assignAnonTag( tag, base ) {
        if ( typeof tag == 'string' ) return tag

        var anon = tag
        var newTag
        if ( base && tag.url && !tag.external && !/[/][/]/.test( tag.url ) ) {
            var m = tag.url.match( /(^|[/])([^/]+)$/ )
            newTag = base + '.' +  m[ 2 ].replace( /[.]/g, '-' ).toLowerCase()
        }
        else {
            newTag = 'anon-' + hash( anon )
        }

        try {
            var inc = includeTag( newTag )
            console.warn( 'tag "' + newTag + '" already defined as', inc, ', will be used instead of', tag )
        }
        catch ( e ) {
            includeTag( newTag, anon )
        }

        return newTag
    }

    function _include( tag ) {
        var inc = includeTag( _assignAnonTag( tag ) )

        if ( inc.include ) return inc.include

        if ( !inc.loader ) {
            var ext = inc.url.match( /[.]([^.]+)$/ )
            if ( ext ) inc.loader = extLoader[ ext[ 1 ] ]
        }

        if ( !loader[ inc.loader ] ) throw new Error( 'tag "' + tag + '" has unknown loader "' + inc.loader + '"' )

        return ( inc.include = new Promise( function ( res, rej ) {
                loader[ inc.loader ].call( loader, inc, tag )
                    .then( function ( r ) {
                        inc.loaded = r

                        if ( !inc.module ) return r

                        return inc.module
                    } )
                    .then( res, rej )

                setTimeout( function () {
                    rej( new Error( 'timeout' ) )
                }, OPTION.timeout )
            } )
            .then( function ( res ) {
                console.debug( 'included ' + inc.loader + ' "' + tag + '"', inc.url || inc.tags )
                return res
            } )
            .catch( function ( e ) {
                e.message += ', for tag "' + tag + '"'
                console.warn(e)

                throw e
            } ) )
    }

    function module( tag, incs, mod ) {
        var inc
        try {
            inc = includeTag( tag )
        }
        catch ( e ) {
            console.warn( 'tag "' + tag + '" for module not defined, creating' )
            inc = includeTag( tag, {} )
        }

        if ( inc.module )
            console.warn( 'tag "' + tag + '" for module already defined, overwriting' )

        var deps
        if ( incs )
            deps = include( incs )
        else
            deps = Promise.resolve()

        return ( inc.module = deps
            .then( function ( res ) {
                if ( typeof mod == 'function' )
                    return mod.call( inc, res )

                return mod
            } )
            .then( function ( exp ) {
                console.debug( 'module "' + tag + '"' )
                inc.exported = exp
                return exp
            } ) )
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Modified from http://stackoverflow.com/a/22429679
     *
     * Calculate a 32 bit FNV-1a hash
     * Found here: https://gist.github.com/vaiorabbit/5657561
     * Ref.: http://isthe.com/chongo/tech/comp/fnv/
     *
     * @param {any} val the input value
     * @returns {string}
     */
    var typeCode = {
        undefined:  '\x00',
        null:       '\x01',
        boolean:    '\x02',
        number:     '\x03',
        string:     '\x04',
        function:   '\x05',
        array:      '\x06',
        object:     '\x0a'
    };

    function type( val ) {
        var t = typeof val
        if ( t != 'object' ) return t
        if ( Array.isArray( val ) ) return 'array'
        if ( val === null ) return 'null'
        return 'object'
    }

    function hash( val ) {
        /* jshint bitwise: false */

        var h = 0x811c9dc5;

        walk( val );

        return ( "0000000" + ( h >>> 0 ).toString( 16 ) ).substr( -8 );

        function walk( val ) {
            var t = type( val );

            switch ( t ) {
            case 'string':
                return addBits( val );

            case 'array':
                addBits( typeCode[ t ] );

                for ( var j1 in val )
                    walk( val[ j1 ] )

                return;

            case 'object':
                addBits( typeCode[ t ] );

                var keys = Object.keys( val ).sort();
                for ( var j2 in keys ) {
                    var key = keys[ j2 ];
                    addBits( key );
                    walk( val[ key ] );
                }
                return;

            case 'undefined':
            case 'null':
                return addBits( typeCode[ t ] )

            default:
                return addBits( typeCode[ t ] + String( val ) )
            }
        }

        function addBits( str ) {
            for ( var i = 0, l = str.length; i < l; i += 1 ) {
                h ^= str.charCodeAt(i);
                h += (h << 1) + (h << 4) + (h << 7) + (h << 8) + (h << 24);
            }
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    window.include = include
    window.include.module = module
    window.include.tag = includeTag
    window.include.hash = hash
    window.include.option = option

} )() }


if ( !window.include.SMK ) { ( function () {
"use strict";

include.tag( "check-directions",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/directions/lib/sortable-1.7.0.min.js",
                "loader": "script"
            },
            {
                "url": "smk/tool/directions/lib/vuedraggable-2.16.0.min.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "check-identify",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/identify/check/check-identify.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "check-query",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/query/check/check-query.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "check-search",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/search/check/check-search.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "check-select",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/select/check/check-select.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "esri3d",
    {
        "loader": "sequence",
        "tags": [
            "leaflet",
            {
                "external": true,
                "url": "https://js.arcgis.com/4.7/esri/css/main.css",
                "loader": "style"
            },
            {
                "external": true,
                "url": "https://js.arcgis.com/4.7/",
                "loader": "script"
            }
        ]
    }
)

include.tag( "event",
    {
        "url": "smk/event.js",
        "loader": "script"
    }
)

include.tag( "feature-list-clustering-leaflet",
    {
        "url": "smk/viewer-leaflet/feature-list-clustering-leaflet.js",
        "loader": "script"
    }
)

include.tag( "feature-list-esri3d",
    {
        "url": "smk/viewer-esri3d/feature-list-esri3d.js",
        "loader": "script"
    }
)

include.tag( "feature-list-leaflet",
    {
        "url": "smk/viewer-leaflet/feature-list-leaflet.js",
        "loader": "script"
    }
)

include.tag( "feature-list",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/feature-list/feature-attributes.html",
                "loader": "template"
            },
            {
                "url": "smk/feature-list/feature-description.html",
                "loader": "template"
            },
            {
                "url": "smk/feature-list/feature-list.js",
                "loader": "script"
            },
            {
                "url": "smk/feature-list/feature-properties.html",
                "loader": "template"
            },
            {
                "url": "smk/feature-list/panel-feature-list.css",
                "loader": "style"
            },
            {
                "url": "smk/feature-list/panel-feature-list.html",
                "loader": "template"
            },
            {
                "url": "smk/feature-list/panel-feature.css",
                "loader": "style"
            },
            {
                "url": "smk/feature-list/panel-feature.html",
                "loader": "template"
            },
            {
                "url": "smk/feature-list/popup-content.html",
                "loader": "template"
            },
            {
                "url": "smk/feature-list/popup-feature.css",
                "loader": "style"
            },
            {
                "url": "smk/feature-list/popup-feature.html",
                "loader": "template"
            }
        ]
    }
)

include.tag( "feature-set",
    {
        "url": "smk/feature-set.js",
        "loader": "script"
    }
)

include.tag( "jquery",
    {
        "url": "lib/jquery-3.3.1.min.js",
        "loader": "script"
    }
)

include.tag( "layer-display",
    {
        "url": "smk/layer-display.js",
        "loader": "script"
    }
)

include.tag( "layer-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/layer/layer-esri-dynamic-esri3d.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-esri3d/layer/layer-vector-esri3d.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-esri3d/layer/layer-wms-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "layer-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/layer/layer-esri-dynamic-leaflet.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-leaflet/layer/layer-vector-leaflet.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-leaflet/layer/layer-wms-leaflet.js",
                "loader": "script"
            },
            {
                "url": "lib/leaflet/marker-cluster-1.0.6.css",
                "loader": "style"
            },
            {
                "url": "lib/leaflet/marker-cluster-1.0.6.js",
                "loader": "script"
            },
            {
                "url": "lib/leaflet/marker-cluster-default-1.0.6.css",
                "loader": "style"
            },
            {
                "url": "lib/leaflet/NonTiledLayer-src.js",
                "loader": "script"
            },
            {
                "url": "lib/leaflet/leaflet-heat.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "layer",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/layer/layer-esri-dynamic.js",
                "loader": "script"
            },
            {
                "url": "smk/layer/layer-vector.js",
                "loader": "script"
            },
            {
                "url": "smk/layer/layer-wms.js",
                "loader": "script"
            },
            {
                "url": "smk/layer/layer.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "leaflet",
    {
        "loader": "sequence",
        "tags": [
            {
                "url": "lib/leaflet/leaflet-1.2.0.min.js",
                "loader": "script"
            },
            {
                "url": "lib/leaflet/leaflet-1.2.0.css",
                "loader": "style"
            },
            {
                "url": "lib/leaflet/esri-leaflet-2.1.0.min.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "material-icons",
    {
        "external": true,
        "url": "https://fonts.googleapis.com/icon?family=Material+Icons",
        "loader": "style"
    }
)

include.tag( "proj4",
    {
        "url": "lib/proj4-2.4.4.min.js",
        "loader": "script"
    }
)

include.tag( "projections",
    {
        "url": "smk/projections.js",
        "loader": "script"
    }
)

include.tag( "query",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/query/query-esri-dynamic.js",
                "loader": "script"
            },
            {
                "url": "smk/query/query-vector.js",
                "loader": "script"
            },
            {
                "url": "smk/query/query-wms.js",
                "loader": "script"
            },
            {
                "url": "smk/query/query.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "sidepanel",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/sidepanel/panel.html",
                "loader": "template"
            },
            {
                "url": "smk/sidepanel/sidepanel.css",
                "loader": "style"
            },
            {
                "url": "smk/sidepanel/sidepanel.html",
                "loader": "template"
            },
            {
                "url": "smk/sidepanel/sidepanel.js",
                "loader": "script"
            },
            "material-icons"
        ]
    }
)

include.tag( "smk-map",
    {
        "url": "smk/smk-map.js",
        "loader": "script"
    }
)

include.tag( "terraformer",
    {
        "loader": "sequence",
        "tags": [
            {
                "url": "lib/terraformer/terraformer-1.0.7.js",
                "loader": "script"
            },
            {
                "url": "lib/terraformer/terraformer-arcgis-parser-1.0.5.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "theme-alpha",
    {
        "loader": "group",
        "tags": [
            {
                "url": "theme/alpha/alpha.css",
                "loader": "style"
            }
        ]
    }
)

include.tag( "theme-base",
    {
        "loader": "group",
        "tags": [
            {
                "url": "theme/_base/command.css",
                "loader": "style"
            },
            {
                "url": "theme/_base/map-frame.css",
                "loader": "style"
            },
            {
                "url": "theme/_base/resets.css",
                "loader": "style"
            },
            {
                "url": "theme/_base/startup.css",
                "loader": "style"
            },
            {
                "url": "theme/_base/variables.css",
                "loader": "style"
            }
        ]
    }
)

include.tag( "theme-beta",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "theme-delta",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "theme-gamma",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-about",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/about/panel-about.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/about/tool-about.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/about/tool-about.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-baseMaps",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/baseMaps/panel-base-maps.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/baseMaps/tool-base-maps.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/baseMaps/tool-baseMaps.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-coordinate-esri3d",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-coordinate-leaflet",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-coordinate",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/coordinate/coordinate.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/coordinate/tool-coordinate.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/coordinate/tool-coordinate.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-directions-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/directions/tool-directions-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-directions-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/directions/tool-directions-leaflet.css",
                "loader": "style"
            },
            {
                "url": "smk/viewer-leaflet/tool/directions/tool-directions-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-directions",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/directions/address-search.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/directions/address-search.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/directions/panel-directions.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/directions/popup-directions.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/directions/tool-directions.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/directions/tool-directions.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-dropdown",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/dropdown/dropdown.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/dropdown/panel-dropdown.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/dropdown/tool-dropdown.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-identify-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/identify/tool-identify-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-identify-feature",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/identify-feature/tool-identify-feature.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-identify-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/identify/tool-identify-leaflet.css",
                "loader": "style"
            },
            {
                "url": "smk/viewer-leaflet/tool/identify/tool-identify-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-identify",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/identify/panel-identify.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/identify/tool-identify.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-layers",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/layers/layer-display.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/layers/panel-layers.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/layers/tool-layers.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/layers/tool-layers.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-list-menu",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/list-menu/list-menu.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/list-menu/panel-list-menu.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/list-menu/panel-tool-list.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/list-menu/tool-list-menu.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-location-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/location/tool-location-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-location-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/location/tool-location-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-location",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/location/panel-location.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/location/popup-location.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/location/tool-location.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/location/tool-location.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-markup-esri3d",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-markup-leaflet",
    {
        "loader": "sequence",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/markup/lib/leaflet-pm-0.17.3.css",
                "loader": "style"
            },
            {
                "url": "smk/viewer-leaflet/tool/markup/lib/leaflet-pm-0.17.3.min.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-leaflet/tool/markup/tool-markup-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-markup",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/markup/tool-markup.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-measure-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/measure/tool-measure-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-measure-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/measure/lib/leaflet-measure.css",
                "loader": "style"
            },
            {
                "url": "smk/viewer-leaflet/tool/measure/lib/leaflet-measure.min.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-leaflet/tool/measure/tool-measure-leaflet.css",
                "loader": "style"
            },
            {
                "url": "smk/viewer-leaflet/tool/measure/tool-measure-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-measure",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/measure/panel-measure.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/measure/tool-measure.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/measure/tool-measure.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-menu",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/menu/menu.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/menu/panel-menu.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/menu/tool-menu.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-minimap-esri3d",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-minimap-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/minimap/lib/Control.MiniMap.min.css",
                "loader": "style"
            },
            {
                "url": "smk/viewer-leaflet/tool/minimap/lib/Control.MiniMap.min.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-leaflet/tool/minimap/tool-minimap-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-minimap",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/minimap/tool-minimap.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-pan-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/pan/tool-pan-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-pan-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/pan/tool-pan-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-pan",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/pan/tool-pan.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-query-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/query/tool-query-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-query-feature",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/query-feature/tool-query-feature.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-query-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/query/tool-query-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-query",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/query/panel-query.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/query/parameter-constant.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/query/parameter-input.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/query/parameter-select.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/query/tool-query.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/query/tool-query.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-scale-esri3d",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-scale-leaflet",
    {
        "loader": "group",
        "tags": []
    }
)

include.tag( "tool-scale",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/scale/scale.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/scale/tool-scale.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/scale/tool-scale.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-search-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/search/tool-search-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-search-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/search/tool-search-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-search-location",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/search-location/address.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/search-location/panel-search-location.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/search-location/tool-search-location.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-search",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/search/panel-search.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/search/popup-search.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/search/tool-search.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/search/tool-search.js",
                "loader": "script"
            },
            {
                "url": "smk/tool/search/widget-search.html",
                "loader": "template"
            }
        ]
    }
)

include.tag( "tool-select-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/select/tool-select-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-select-feature",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/select-feature/tool-select-feature.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-select-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/select/tool-select-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-select",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/select/panel-select.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/select/tool-select.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-shortcut-menu",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/shortcut-menu/shortcut-menu.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/shortcut-menu/shortcut-menu.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/shortcut-menu/tool-shortcut-menu.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-toolbar",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/toolbar/tool-toolbar.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-version",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/version/panel-version.html",
                "loader": "template"
            },
            {
                "url": "smk/tool/version/tool-version.css",
                "loader": "style"
            },
            {
                "url": "smk/tool/version/tool-version.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-zoom-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/tool/zoom/tool-zoom-esri3d.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-zoom-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/tool/zoom/tool-zoom-leaflet.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool-zoom",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/tool/zoom/tool-zoom.js",
                "loader": "script"
            }
        ]
    }
)

include.tag( "tool",
    {
        "url": "smk/tool.js",
        "loader": "script"
    }
)

include.tag( "toolbar",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/toolbar/toolbar.css",
                "loader": "style"
            },
            {
                "url": "smk/toolbar/toolbar.html",
                "loader": "template"
            },
            {
                "url": "smk/toolbar/toolbar.js",
                "loader": "script"
            },
            "material-icons"
        ]
    }
)

include.tag( "turf",
    {
        "url": "lib/turf-5.1.6.min.js",
        "loader": "script"
    }
)

include.tag( "types-esri3d",
    {
        "url": "smk/viewer-esri3d/types-esri3d.js",
        "loader": "script"
    }
)

include.tag( "util-esri3d",
    {
        "url": "smk/viewer-esri3d/util-esri3d.js",
        "loader": "script"
    }
)

include.tag( "util",
    {
        "url": "smk/util.js",
        "loader": "script"
    }
)

include.tag( "viewer-esri3d",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-esri3d/viewer-esri3d.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-esri3d/viewer-esri3d.css",
                "loader": "style"
            }
        ]
    }
)

include.tag( "viewer-leaflet",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/viewer-leaflet/viewer-leaflet.js",
                "loader": "script"
            },
            {
                "url": "smk/viewer-leaflet/viewer-leaflet.css",
                "loader": "style"
            }
        ]
    }
)

include.tag( "viewer",
    {
        "url": "smk/viewer.js",
        "loader": "script"
    }
)

include.tag( "vue-config",
    {
        "url": "smk/vue-config.js",
        "loader": "script"
    }
)

include.tag( "vue",
    {
        "url": "lib/vue-2.5.11.js",
        "loader": "script"
    }
)

include.tag( "widgets",
    {
        "loader": "group",
        "tags": [
            {
                "url": "smk/widgets/tool-button.html",
                "loader": "template"
            },
            {
                "url": "smk/widgets/widgets.css",
                "loader": "style"
            },
            {
                "url": "smk/widgets/widgets.js",
                "loader": "script"
            }
        ]
    }
)


window.include.SMK = true
} )() }

( function () {
    "use strict";

    function isIE() {
        return navigator.userAgent.indexOf( "MSIE " ) > -1 || navigator.userAgent.indexOf( "Trident/" ) > -1;
    }

    try {
        if ( isIE() )
            throw new Error( 'This map will not function in Internet Explorer 11. Please use a more modern browser such as Google Chrome, Microsoft Edge, Firefox, or Safari.' )

        var util = {}
        installPolyfills( util )
        setupGlobalSMK( util )

        var documentReadyPromise

        var bootstrapScriptEl = document.currentScript

        var timer
        SMK.BOOT = SMK.BOOT
            .then( parseScriptElement )
            .then( function ( attr ) {
                timer = 'SMK initialize ' + attr.id
                console.time( timer )
                return attr
            } )
            .then( resolveConfig )
            .then( initializeSmkMap )
            .catch( SMK.ON_FAILURE )

        util.promiseFinally( SMK.BOOT, function () {
            console.timeEnd( timer )
        } )
    }
    catch ( e ) {
        setTimeout( function () {
            document.querySelector( 'body' ).appendChild( failureMessage( e ) )
        }, 1000 )
    }

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    function parseScriptElement() {
        var smkAttr = {
            'id':           attrString( '1' ),
            'container-sel':attrString( '#smk-map-frame' ),
            'title-sel':    attrString( 'head title' ),
            'config':       attrList( '?smk-' ),
            'base-url':     attrString( ( new URL( bootstrapScriptEl.src.replace( 'smk.js', '' ), document.location ) ).toString() ),
            'service-url':  attrString( null, null ),
        }

        Object.keys( smkAttr ).forEach( function ( k ) {
            smkAttr[ k ] = smkAttr[ k ]( 'smk-' + k, bootstrapScriptEl )
        } )

        console.log( 'SMK attributes', JSON.parse( JSON.stringify( smkAttr ) ) )

        return Promise.resolve( smkAttr )

        // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

        function attrString( missingKey, missingValue ) {
            if ( missingValue === undefined )
                missingValue = missingKey

            return function( key, el ) {
                var val = el.attributes[ key ]
                if ( val == null ) return missingKey
                if ( !val.value ) return missingValue
                return val.value
            }
        }

        function attrList( Default ) {
            return function( key, el ) {
                var val = attrString( Default )( key, el )
                return val.split( /\s*[|]\s*/ ).filter( function ( i ) { return !!i } )
            }
        }

        function attrBoolean( missingKey, missingValue ) {
            /* jshint evil: true */

            return function( key, el ) {
                var val = attrString( missingKey, missingValue )( key, el )
                return !!eval( val )
            }
        }
    }

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    function resolveConfig( attr ) {
        var configs = []
        attr.config.forEach( function ( c, i ) {
            var source = 'attr[' + i + ']'
            configs = configs.concat( parseDocumentArguments( c, source ) || parseLiteralJson( c, source ) || parseOption( c, source ) || parseUrl( c, source ) )
        } )

        attr.config = configs

        return attr
    }

    function parseDocumentArguments( config, source ) {
        if ( !/^[?]/.test( config ) ) return

        var paramPattern = new RegExp( '^' + config.substr( 1 ) + '(.+)$', 'i' )

        var params = location.search.substr( 1 ).split( '&' )
        var configs = []
        params.forEach( function ( p, i ) {
            var source1 = source + ' -> arg[' + config + ',' + i + ']'
            try {
                var m = decodeURIComponent( p ).match( paramPattern )
                if ( !m ) return

                configs = configs.concat( parseOption( m[ 1 ], source1 ) || [] )
            }
            catch ( e ) {
                if ( !e.parseSource ) e.parseSource = source1
                throw e
            }
        } )

        return configs
    }

    function parseLiteralJson( config, source ) {
        if ( !/^[{].+[}]$/.test( config ) ) return

        source += ' -> json'
        try {
            var obj = JSON.parse( config )
            obj.$sources = [ source ]

            return obj
        }
        catch ( e ) {
            if ( !e.parseSource ) e.parseSource = source
            throw e
        }
    }

    function parseOption( config, source ) {
        var m = config.match( /^(.+?)([=](.+))?$/ )
        if ( !m ) return

        var option = m[ 1 ].toLowerCase()
        if ( !( option in optionHandler ) ) return

        source += ' -> option[' + option + ']'
        try {
            var obj = optionHandler[ option ]( m[ 3 ], source )
            if ( !obj.$sources )
                obj.$sources = [ source ]

            return obj
        }
        catch ( e ) {
            if ( !e.parseSource ) e.parseSource = source
            throw e
        }
    }

    function parseUrl( config, source ) {
        source += ' -> url[' + config + ']'
        var obj = {
            url: config,
            $sources: [ source ]
        }

        return obj
    }

    var optionHandler = {
        'config': function ( arg, source ) {
            return parseLiteralJson( arg, source ) || parseUrl( arg, source )
        },

        'theme': function ( arg, source ) {
            var args = arg.split( ',' )
            if ( args.length != 1 ) throw new Error( '-theme needs at least 1 argument' )
            return {
                viewer: {
                    themes: args
                }
            }
        },

        'device': function ( arg, source ) {
            var args = arg.split( ',' )
            if ( args.length != 1 ) throw new Error( '-device needs 1 argument' )
            return {
                viewer: {
                    device: args[ 0 ]
                }
            }
        },

        'extent': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length != 4 ) throw new Error( '-extent needs 4 arguments' )
            return {
                viewer: {
                    location: {
                        extent: args,
                        center: null,
                        zoom: null,
                    }
                }
            }
        },

        'center': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length < 2 || args.length > 3 ) throw new Error( '-center needs 2 or 3 arguments' )

            var loc = {
                center: [ args[ 0 ], args[ 1 ] ],
            }

            if ( args[ 2 ] )
                loc.zoom = args[ 2 ]

            return {
                viewer: {
                    location: loc
                }
            }
        },

        'viewer': function ( arg ) {
            return {
                viewer: {
                    type: arg
                }
            }
        },

        'active-tool': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length != 1 && args.length != 2 ) throw new Error( '-active-tool needs 1 or 2 arguments' )

            var toolId = args[ 0 ]
            if ( args[ 1 ] )
                toolId += '--' + args[ 1 ]

            return {
                viewer: {
                    activeTool: toolId
                }
            }
        },

        'query': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length < 3 ) throw new Error( '-query needs at least 3 arguments' )

            var queryId = 'query-' + arg.replace( /[^a-z0-9]+/ig, '-' ).replace( /(^[-]+)|([-]+$)/g, '' ).toLowerCase()

            var layerId = args[ 0 ]
            var conj = args[ 1 ].trim().toLowerCase()
            if ( conj != 'and' && conj != 'or' ) throw new Error( '-query conjunction must be one of: AND, OR' )

            var parameters = []
            function constant( value ) {
                var id = 'constant' + ( parameters.length + 1 )
                parameters.push( {
                    id: id,
                    type: 'constant',
                    value: JSON.parse( value )
                } )
                return id
            }

            var clauses = args.slice( 2 ).map( function ( p ) {
                var m = p.trim().match( /^(\w+)\s*([$^]?~|=|<=?|>=?)\s*(.+?)$/ )
                if ( !m ) throw new Error( '-query expression is invalid' )

                var args = [
                    { operand: 'attribute', name: m[ 1 ] },
                    { operand: 'parameter', id: constant( m[ 3 ] ) }
                ]

                switch ( m[ 2 ].toLowerCase() ) {
                    case '~':  return { operator: 'contains', arguments: args }
                    case '^~': return { operator: 'starts-with', arguments: args }
                    case '$~': return { operator: 'ends-with', arguments: args }
                    case '=':  return { operator: 'equals', arguments: args }
                    case '>':  return { operator: 'greater-than', arguments: args }
                    case '<':  return { operator: 'less-than', arguments: args }
                    case '>=': return { operator: 'not', arguments: [ { operator: 'less-than', arguments: args } ] }
                    case '<=': return { operator: 'not', arguments: [ { operator: 'greater-than', arguments: args } ] }
                }
            } )

            return {
                viewer: {
                    activeTool: 'query--' + layerId + '--' + queryId,
                },
                tools: [ {
                    type: 'query',
                    instance: layerId + '--' + queryId,
                    onActivate: 'execute'
                } ],
                layers: [ {
                    id: layerId,
                    queries: [ {
                        id: queryId,
                        parameters: parameters,
                        predicate: {
                            operator: conj,
                            arguments: clauses
                        }
                    } ]
                } ]
            }
        },

        'layer': function ( arg, source ) {
            var args = arg.split( ',' )
            if ( args.length < 2 ) throw new Error( '-layer needs at least 2 arguments' )

            var layerId = 'layer-' + arg.replace( /[^a-z0-9]+/ig, '-' ).replace( /(^[-]+)|([-]+$)/g, '' ).toLowerCase()

            var type = args[ 0 ].trim().toLowerCase()
            switch ( type ) {
                case 'esri-dynamic':
                    if ( args.length < 3 ) throw new Error( '-layer=esri-dynamic needs at least 3 arguments' )
                    return {
                        layers: [ {
                            id:         layerId,
                            type:       'esri-dynamic',
                            isVisible:  true,
                            serviceUrl: args[ 1 ],
                            mpcmId:     args[ 2 ],
                            title:      args[ 3 ] || ( 'ESRI Dynamic ' + args[ 2 ] ),
                        } ]
                }

                case 'wms':
                    if ( args.length < 3 ) throw new Error( '-layer=wms needs at least 3 arguments' )
                    return {
                        layers: [ {
                            id:         layerId,
                            type:       'wms',
                            isVisible:  true,
                            serviceUrl: args[ 1 ],
                            layerName:  args[ 2 ],
                            styleName:  args[ 3 ],
                            title:      args[ 4 ] || ( 'WMS ' + args[ 2 ] ),
                        } ]
                }

                case 'vector':
                    return {
                        layers: [ {
                            id:         layerId,
                            type:       'vector',
                            isVisible:  true,
                            dataUrl:    args[ 1 ],
                            title:      args[ 2 ] || ( 'Vector ' + args[ 1 ] ),
                        } ]
                    }

                default: throw new Error( 'unknown layer type: ' + type )
            }
        },

        'show-tool': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length < 1 ) throw new Error( '-show-tool needs at least 1 argument' )

            return {
                tools: args.map( function ( type ) {
                    if ( type == 'all' ) type = '*'
                    return {
                        type: type,
                        enabled: true
                    }
                } )
            }
        },

        'hide-tool': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length < 1 ) throw new Error( '-hide-tool needs at least 1 argument' )

            return {
                tools: args.map( function ( type ) {
                    if ( type == 'all' ) type = '*'
                    return {
                        type: type,
                        enabled: false
                    }
                } )
            }
        },

        'show-layer': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length < 1 ) throw new Error( '-show-layer needs at least 1 argument' )

            return {
                layers: args.map( function ( id ) {
                    if ( id == 'all' ) id = '**'
                    return {
                        id: id,
                        isVisible: true
                    }
                } )
            }
        },

        'hide-layer': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length < 1 ) throw new Error( '-hide-layer needs at least 1 argument' )

            return {
                layers: args.map( function ( id ) {
                    if ( id.toLowerCase() == 'all' ) id = '**'
                    return {
                        id: id,
                        isVisible: false
                    }
                } )
            }
        },

        // Options below are for backward compatibility with DMF

        'll': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length != 2 ) throw new Error( '-ll needs 2 arguments' )

            return {
                viewer: {
                    location: {
                        center: [ args[ 0 ], args[ 1 ] ]
                    }
                }
            }
        },

        'z': function ( arg ) {
            var args = arg.split( ',' )
            if ( args.length != 1 ) throw new Error( '-z needs 1 argument' )

            return {
                viewer: {
                    location: {
                        zoom: args[ 0 ]
                    }
                }
            }
        },

    }

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    function initializeSmkMap( attr ) {
        include.option( { baseUrl: attr[ 'base-url' ] } )

        return whenDocumentReady()
            .then( function () {
                if ( window.jQuery ) {
                    include.tag( 'jquery' ).include = Promise.resolve( window.jQuery )
                    return
                }

                return include( 'jquery' )
            } )
            .then( function () {
                if ( window.Vue ) {
                    include.tag( 'vue' ).include = Promise.resolve( window.Vue )
                    return
                }

                return include( 'vue' )
            } )
            .then( function () {
                return include( 'vue-config' )
            } )
            .then( function () {
                if ( window.turf ) {
                    include.tag( 'turf' ).include = Promise.resolve( window.turf )
                    return
                }

                return include( 'turf' )
            } )
            .then( function () {
                return include( 'smk-map' ).then( function ( inc ) {
                    if ( attr[ 'id' ] in SMK.MAP )
                        throw new Error( 'An SMK map with smk-id "' + attr[ 'id' ] + '" already exists' )

                    var map = SMK.MAP[ attr[ 'id' ] ] = new SMK.TYPE.SmkMap( attr )
                    return map.initialize()
                } )
            } )
    }

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    function whenDocumentReady() {
        if ( documentReadyPromise ) return documentReadyPromise

        return ( documentReadyPromise = new Promise( function ( res, rej ) {
            if ( document.readyState != "loading" )
                return res()

            document.addEventListener( "DOMContentLoaded", function( ev ) {
                clearTimeout( id )
                res()
            } )

            var id = setTimeout( function () {
                console.error( 'timeout waiting for document ready' )
                rej()
            }, 20000 )
        } ) )
    }

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    function installPolyfills( util ) {
        window.dojoConfig = {
            has: {
                "esri-promise-compatibility": 1
            }
        }

        // - - - - - - - - - - - - - - - - - - - - -
        // document.currentScript polyfill for IE11
        // - - - - - - - - - - - - - - - - - - - - -
        if ( !document.currentScript ) ( function() {
            var scripts = document.getElementsByTagName( 'script' )
            // document._currentScript = document.currentScript

            // return script object based off of src
            var getScriptFromURL = function( url ) {
                // console.log( url )
                for ( var i = 0; i < scripts.length; i += 1 )
                    if ( scripts[ i ].src == url ) {
                        // console.log( scripts[ i ] )
                        return scripts[ i ]
                    }
            }

            var actualScript = document.actualScript = function() {
                /* jshint -W030 */ // Expected an assignment or function call and instead saw an expression.
                /* jshint -W117 */ // omgwtf is not defined

                var stack
                try {
                    omgwtf
                } catch( e ) {
                    stack = e.stack
                }

                if ( !stack ) return

                var entries = stack.split( /\s+at\s+/ )
                var last = entries[ entries.length - 1 ]

                var m = last.match( /[(](.+?)(?:[:]\d+)+[)]/ )
                if ( m )
                    return getScriptFromURL( m[ 1 ] )
            }

            if ( document.__defineGetter__ )
                document.__defineGetter__( 'currentScript', actualScript )
        } )()


        if ( Promise.prototype.finally )
            util.promiseFinally = function ( promise, onFinally ) {
                return promise.finally( onFinally )
            }
        else
            util.promiseFinally = function ( promise, onFinally ) {
                var onThen = function ( arg ) {
                    onFinally()
                    return arg
                }

                var onFail = function ( arg ) {
                    onFinally()
                    return Promise.reject( arg )
                }

                return promise.then( onThen, onFail )
            }

    }

// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =

    function setupGlobalSMK( util ) {
        return ( window.SMK = Object.assign( {
            MAP: {},
            VIEWER: {},
            TYPE: {},
            UTIL: util,

            CONFIG: {
                name: 'SMK Default Map',
                viewer: {
                    type: "leaflet",
                    device: "auto",
                    deviceAutoBreakpoint: 500,
                    themes: [],
                    location: {
                        extent: [ -139.1782, 47.6039, -110.3533, 60.5939 ],
                    },
                    baseMap: 'Topographic'
                },
                tools: [
                    { type: 'about',        enabled: false },
                    { type: 'baseMaps',     enabled: false },
                    { type: 'coordinate',   enabled: false },
                    { type: 'directions',   enabled: false },
                    // { type: 'dropdown',     enabled: false }, -- so it won't be enabled by show-tools=all, no tools use it by default
                    { type: 'identify',     enabled: false },
                    { type: 'layers',       enabled: false },
                    { type: 'location',     enabled: true },
                    { type: 'markup',       enabled: true },
                    { type: 'measure',      enabled: false },
                    { type: 'menu',         enabled: false },
                    { type: 'minimap',      enabled: false },
                    { type: 'pan',          enabled: false },
                    // { type: 'query',        enabled: false }, -- so it won't be enabled by show-tools=all, as it needs an instance
                    { type: 'scale',        enabled: false },
                    { type: 'search',       enabled: true },
                    { type: 'select',       enabled: false },
                    { type: 'toolbar',      enabled: true },
                    // { type: 'version',      enabled: false }, -- so it won't be enabled by show-tools=all
                    { type: 'zoom',         enabled: false }
                ]
            },

            BOOT: Promise.resolve(),
            TAGS_DEFINED: false,
            ON_FAILURE: function ( e ) {
                if ( !e ) return

                if ( e.parseSource )
                    e.message += ',\n  while parsing ' + e.parseSource

                console.error( e )

                var message = document.createElement( 'div' )
                message.innerHTML = '\
                    <div style="\
                        display:flex;\
                        flex-direction:column;\
                        justify-content:center;\
                        align-items:center;\
                        border: 5px solid red;\
                        padding: 20px;\
                        margin: 20px;\
                        position: absolute;\
                        top: 0;\
                        left: 0;\
                        right: 0;\
                        bottom: 0;\
                    ">\
                        <h1>SMK Client</h1>\
                        <h2>Initialization failed</h2>\
                        <pre style="white-space: normal">{}</pre>\
                    </div>\
                '.replace( /\s+/g, ' ' ).replace( '{}', e || '' )

                whenDocumentReady().then( function () {
                    document.querySelector( 'body' ).appendChild( message )
                } )
            },

            BUILD: {
                commit:     '',
                branch:     '',
                lastCommit: ''.replace( /^"|"$/g, '' ),
                origin:     '',
                version:    '',
            }

        }, window.SMK ) )
    }

    function failureMessage( e ) {
        if ( e.parseSource )
            e.message += ',\n  while parsing ' + e.parseSource

        console.error( e )

        var message = document.createElement( 'div' )
        message.innerHTML = '\
            <div style="\
                display:flex;\
                flex-direction:column;\
                justify-content:center;\
                align-items:center;\
                border: 5px solid red;\
                padding: 20px;\
                margin: 20px;\
                position: absolute;\
                top: 0;\
                left: 0;\
                right: 0;\
                bottom: 0;\
            ">\
                <h1>SMK Client</h1>\
                <h2>Initialization failed</h2>\
                <pre style="white-space: normal">{}</pre>\
            </div>\
        '.replace( /\s+/g, ' ' ).replace( '{}', e || '' )

        return message
    }







} )();

