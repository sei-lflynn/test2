import $ from 'jquery'
import * as d3 from 'd3'
import F_ from '../Formulae_/Formulae_'
import L_ from '../Layers_/Layers_'
import { captureVector } from '../Layers_/LayerCapturer'
import {
    constructVectorLayer,
    constructSublayers,
} from '../Layers_/LayerConstructors'
import Filtering from '../Layers_/Filtering/Filtering'
import Viewer_ from '../Viewer_/Viewer_'
import Globe_ from '../Globe_/Globe_'
import ToolController_ from '../ToolController_/ToolController_'
import CursorInfo from '../../Ancillary/CursorInfo'
import Description from '../../Ancillary/Description'
import QueryURL from '../../Ancillary/QueryURL'
import MetadataCapturer from '../Layers_/MetadataCapturer.js'
import { Kinds } from '../../../pre/tools'
import DataShaders from '../../Ancillary/DataShaders'
import calls from '../../../pre/calls'
import TimeControl from '../../Ancillary/TimeControl'

import gjv from 'geojson-validation'
import {
    evaluate_cmap,
    data as colormapData,
} from '../../../external/js-colormaps/js-colormaps.js'

let L = window.L

let essenceFina = function () {}

import GeoRasterLayer from '../../../external/georaster-layer-for-leaflet/georaster-layer-for-leaflet.ts'
import georaster from 'georaster'

// The default color ramp used for image layer types
const IMAGE_DEFAULT_COLOR_RAMP = 'binary'

let Map_ = {
    //Our main leaflet map variable
    map: null,
    toolbar: null,
    tempOverlayImage: null,
    activeLayer: null,
    allLayersLoadedPassed: false,
    player: { arrow: null, lookat: null },
    //Initialize a map based on a config file
    init: function (essenceFinal) {
        essenceFina = essenceFinal

        //Repair Leaflet and plugin incongruities
        L.DomEvent._fakeStop = L.DomEvent.fakeStop

        //var fakeStop = L.DomEvent.fakeStop || L.DomEvent._fakeStop || stop;?
        /*
            var xhr = new XMLHttpRequest();
            try {
              xhr.open("GET", 'Missions/MTTT/Layers/TEMP/M2020_EDL_bufpoints_3m_geo/12/2929/1834.pbf');
              xhr.responseType = "arraybuffer";
              xhr.onerror = function() {
                console.log("Network error")
              };
              xhr.onload = function() {
                if (xhr.status === 200) {
                    var data = new Pbf(new Uint8Array(xhr.response)).readFields(readData, {});

                    console.log( data )

                    function readData(tag, data, pbf) {
                        if (tag === 1) data.name = pbf.readString();
                        else if (tag === 2) data.version = pbf.readVarint();
                        //else if (tag === 3) data.layer = pbf.readMessage(readLayer, {});
                    }
                    function readLayer(tag, layer, pbf) {
                        if (tag === 1) layer.name = pbf.readString();
                        else if (tag === 3) layer.size = pbf.readVarint();
                    }
                }
                else console.log(xhr.statusText);
                
              };
              xhr.send();
            } catch (err) {
              console.log(err.message)
            }
            */

        var hasZoomControl = false
        if (L_.configData.look && L_.configData.look.zoomcontrol)
            hasZoomControl = true

        Map_.mapScaleZoom = L_.configData.msv.mapscale || null

        if (this.map != null) this.map.remove()

        let shouldFade = true

        let maxBounds = null
        if (
            !isNaN(L_.configData.msv.maxBoundsTopLeftLat) &&
            !isNaN(L_.configData.msv.maxBoundsTopLeftLng) &&
            !isNaN(L_.configData.msv.maxBoundsBottomRightLat) &&
            !isNaN(L_.configData.msv.maxBoundsBottomRightLng) &&
            !(
                L_.configData.msv.maxBoundsTopLeftLat === 0 &&
                L_.configData.msv.maxBoundsTopLeftLng === 0 &&
                L_.configData.msv.maxBoundsBottomRightLat === 0 &&
                L_.configData.msv.maxBoundsBottomRightLng === 0
            )
        ) {
            maxBounds = [
                [
                    L_.configData.msv.maxBoundsTopLeftLat,
                    L_.configData.msv.maxBoundsTopLeftLng,
                ],
                [
                    L_.configData.msv.maxBoundsBottomRightLat,
                    L_.configData.msv.maxBoundsBottomRightLng,
                ],
            ]
        }

        if (
            L_.configData.projection &&
            L_.configData.projection.custom === true
        ) {
            var cp = L_.configData.projection
            //console.log(cp)
            var crs = new L.Proj.CRS(
                Number.isFinite(parseInt(cp.epsg[0]))
                    ? `EPSG:${cp.epsg}`
                    : cp.epsg,
                cp.proj,
                {
                    origin: [
                        parseFloat(cp.origin[0]),
                        parseFloat(cp.origin[1]),
                    ],
                    resolutions: cp.res,
                    bounds: L.bounds(
                        [parseFloat(cp.bounds[0]), parseFloat(cp.bounds[1])],
                        [parseFloat(cp.bounds[2]), parseFloat(cp.bounds[3])]
                    ),
                },
                parseFloat(L_.configData.msv.radius.major)
            )
            crs.projString = cp.proj

            this.map = L.map('map', {
                zoomControl: hasZoomControl,
                editable: true,
                keyboard: false,
                crs: crs,
                zoomDelta: 0.05,
                zoomSnap: 0,
                fadeAnimation: shouldFade,
                //wheelPxPerZoomLevel: 500,
                worldCopyJump: L_.configData.msv.worldCopyJump || false,
                maxBounds,
            })

            window.mmgisglobal.customCRS = crs
        } else {
            //Make the empty map and turn off zoom controls
            this.map = L.map('map', {
                zoomControl: hasZoomControl,
                editable: true,
                keyboard: false,
                fadeAnimation: shouldFade,
                //crs: crs,
                //zoomDelta: 0.05,
                //zoomSnap: 0,
                //wheelPxPerZoomLevel: 500,
                worldCopyJump: L_.configData.msv.worldCopyJump || false,
                maxBounds,
            })
            // Default CRS

            const projString = `+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +a=${F_.radiusOfPlanetMajor} +b=${F_.radiusOfPlanetMinor} +towgs84=0,0,0,0,0,0,0 +units=m +no_defs`
            window.mmgisglobal.customCRS = new L.Proj.CRS(
                'EPSG:3857',
                projString,
                null,
                F_.radiusOfPlanetMajor
            )
            window.mmgisglobal.customCRS.projString = projString
        }

        if (this.map.zoomControl) this.map.zoomControl.setPosition('topright')

        if (Map_.mapScaleZoom) {
            L.control
                .scalefactor({
                    radius: parseInt(L_.configData.msv.radius.major),
                    mapScaleZoom: Map_.mapScaleZoom,
                })
                .addTo(this.map)
        }

        //Initialize the view to that set in config
        if (L_.FUTURES.mapView != null) {
            this.resetView(L_.FUTURES.mapView)
            if (L_.FUTURES.centerPin != null) {
                this._centerPin = new L.circleMarker(
                    [L_.FUTURES.mapView[0], L_.FUTURES.mapView[1]],
                    {
                        fillColor: '#000',
                        fillOpacity: 0,
                        color: 'lime',
                        weight: 2,
                    }
                )
                    .setRadius(4)
                    .addTo(this.map)
                if (
                    L_.FUTURES.centerPin.length > 0 &&
                    L_.FUTURES.centerPin != 'true'
                ) {
                    this._centerPin.on('mouseover', function () {
                        CursorInfo.update(L_.FUTURES.centerPin, null, false)
                    })
                    this._centerPin.on('mouseout', function () {
                        CursorInfo.hide()
                    })
                }
            }
        } else {
            this.resetView(L_.view)
        }

        //Remove attribution
        d3.select('.leaflet-control-attribution').remove()

        //Make our layers
        makeLayers(L_.layers.dataFlat)

        //Just in case we have no layers
        allLayersLoaded()

        //Add a graticule
        if (L_.configData.look && L_.configData.look.graticule == true) {
            this.toggleGraticule(true)
        }

        //When done zooming, hide the things you're too far out to see/reveal the things you're close enough to see
        this.map.on('zoomend', function () {
            L_.enforceVisibilityCutoffs()

            // Set all zoom elements
            $('.map-autoset-zoom').text(Map_.map.getZoom())
        })

        this.map.on('movestart', fadeOutCertainLayers)
        this.map.on('zoomstart', fadeOutCertainLayers)

        function fadeOutCertainLayers() {
            // Fade out Velocity layer Streamlines to prevent rendering jumps
            Object.keys(L_.layers.data).forEach((layerUUID) => {
                const layerData = L_.layers.data[layerUUID]
                if (
                    layerData.type === 'velocity' &&
                    (layerData.kind === 'streamlines' || layerData.kind == null)
                ) {
                    L_.layers.layer[layerUUID].setOpacity(0)
                }
            })
        }

        if (Globe_.controls.link) {
            this.map.on('move', (e) => {
                const c = this.map.getCenter()
                Globe_.controls.link.linkMove(c.lng, c.lat)
            })
            this.map.on('mousemove', (e) => {
                Globe_.controls.link.linkMouseMove(e.latlng.lng, e.latlng.lat)
            })
            this.map.on('mouseout', (e) => {
                Globe_.controls.link.linkMouseOut()
            })
        }

        // Clear the selected feature if clicking on the map where there are no features
        Map_.map.addEventListener('click', clearOnMapClick)

        //Build the toolbar
        buildToolBar()

        //Set the time for any time enabled layers
        TimeControl.updateLayersTime()
    },
    toggleGraticule: function (on) {
        if (on)
            this.graticule = L.latlngGraticule({
                showLabel: true,
                color: 'rgba(255,255,255,0.75)',
                weight: 1,
                zoomInterval: [
                    { start: 2, end: 3, interval: 40 },
                    { start: 4, end: 5, interval: 20 },
                    { start: 6, end: 7, interval: 10 },
                    { start: 8, end: 9, interval: 5 },
                    { start: 10, end: 11, interval: 0.4 },
                    { start: 12, end: 13, interval: 0.2 },
                    { start: 14, end: 15, interval: 0.1 },
                    { start: 16, end: 17, interval: 0.01 },
                    { start: 18, end: 19, interval: 0.005 },
                    { start: 20, end: 21, interval: 0.0025 },
                    { start: 21, end: 30, interval: 0.00125 },
                ],
            }).addTo(Map_.map)
        else {
            this.rmNotNull(this.graticule)
            this.graticule = null
        }
    },
    clear: function () {
        this.map.eachLayer(function (layer) {
            Map_.map.removeLayer(layer)
        })

        this.toolbar = null
        this.tempOverlayImage = null
        this.activeLayer = null
        this.allLayersLoadedPassed = false
        this.player = { arrow: null, lookat: null }
    },
    setZoomToMapScale() {
        this.map.setZoom(this.mapScaleZoom)
    },
    //Focuses the map on [lat, lon, zoom]
    resetView: function (latlonzoom, stopNextMove) {
        //Uses Leaflet's setView
        var lat = parseFloat(latlonzoom[0])
        if (isNaN(lat)) lat = 0
        var lon = parseFloat(latlonzoom[1])
        if (isNaN(lon)) lon = 0
        var zoom = parseInt(latlonzoom[2])
        if (zoom == null || isNaN(zoom))
            zoom =
                this.map.getZoom() ||
                L_.configData.msv.mapscale ||
                L_.configData.msv.view[2]
        this.map.setView([lat, lon], zoom)
        this.map.invalidateSize()
    },
    //returns true if the map has the layer
    hasLayer: function (layername) {
        if (L_.layers.layer[layername]) {
            return Map_.map.hasLayer(L_.layers.layer[layername])
        }
        return false
    },
    //adds a temp tile layer to the map
    tempTileLayer: null,
    changeTempTileLayer: function (url) {
        this.removeTempTileLayer()
        this.tempTileLayer = L.tileLayer(url, {
            minZoom: 0,
            maxZoom: 25,
            maxNativeZoom: 25,
            tms: true, //!!!
            noWrap: true,
            continuousWorld: true,
            reuseTiles: true,
        }).addTo(this.map)
    },
    //removes that layer
    removeTempTileLayer: function () {
        this.rmNotNull(this.tempTileLayer)
    },
    //Removes the map layer if it isn't null
    rmNotNull: function (layer) {
        if (layer != null) {
            this.map.removeLayer(layer)
            layer = null
        }
    },
    //Redraws all layers, starting with the bottom one
    orderedBringToFront: function () {
        let hasIndex = []
        let hasIndexRaster = []

        for (let i = L_._layersOrdered.length - 1; i >= 0; i--) {
            if (Map_.hasLayer(L_._layersOrdered[i])) {
                if (L_.layers.data[L_._layersOrdered[i]]) {
                    if (
                        L_.layers.data[L_._layersOrdered[i]].type === 'vector'
                    ) {
                        if (L_.layers.attachments[L_._layersOrdered[i]]) {
                            for (let s in L_.layers.attachments[
                                L_._layersOrdered[i]
                            ]) {
                                Map_.rmNotNull(
                                    L_.layers.attachments[L_._layersOrdered[i]][
                                        s
                                    ].layer
                                )
                            }
                        }
                        Map_.map.removeLayer(
                            L_.layers.layer[L_._layersOrdered[i]]
                        )
                        hasIndex.push(i)
                    } else if (
                        L_.layers.data[L_._layersOrdered[i]].type === 'tile' ||
                        L_.layers.data[L_._layersOrdered[i]].type === 'data'
                    ) {
                        hasIndexRaster.push(i)
                    } else if (
                        L_.layers.data[L_._layersOrdered[i]].type === 'image'
                    ) {
                        Map_.map.removeLayer(
                            L_.layers.layer[L_._layersOrdered[i]]
                        )
                        hasIndex.push(i)
                    }
                }
            }
        }

        // First only vectors and images
        for (let i = 0; i < hasIndex.length; i++) {
            if (L_.layers.attachments[L_._layersOrdered[hasIndex[i]]]) {
                for (let s in L_.layers.attachments[
                    L_._layersOrdered[hasIndex[i]]
                ]) {
                    if (
                        L_.layers.attachments[L_._layersOrdered[hasIndex[i]]][s]
                            .on
                    ) {
                        if (
                            L_.layers.attachments[
                                L_._layersOrdered[hasIndex[i]]
                            ][s].type !== 'model'
                        ) {
                            Map_.map.addLayer(
                                L_.layers.attachments[
                                    L_._layersOrdered[hasIndex[i]]
                                ][s].layer
                            )
                        }
                    }
                }
            }

            Map_.map.addLayer(L_.layers.layer[L_._layersOrdered[hasIndex[i]]])

            // If image layer, reorder the z index and redraw the layer
            if (
                L_.layers.data[L_._layersOrdered[hasIndex[i]]].type === 'image'
            ) {
                L_.layers.layer[L_._layersOrdered[hasIndex[i]]].setZIndex(
                    L_._layersOrdered.length +
                        1 -
                        L_._layersOrdered.indexOf(
                            L_._layersOrdered[hasIndex[i]]
                        )
                )
                L_.layers.layer[L_._layersOrdered[hasIndex[i]]].clearCache()
                L_.layers.layer[L_._layersOrdered[hasIndex[i]]].redraw()
            }
        }

        L_.enforceVisibilityCutoffs()

        // Now only rasters
        // They're separate because its better to only change the raster z-index
        for (let i = 0; i < hasIndexRaster.length; i++) {
            L_.layers.layer[L_._layersOrdered[hasIndexRaster[i]]].setZIndex(
                L_._layersOrdered.length +
                    1 -
                    L_._layersOrdered.indexOf(
                        L_._layersOrdered[hasIndexRaster[i]]
                    )
            )
        }

        // Now bring any Drawn layers back to the front:
        Object.keys(L_.layers.layer).forEach((key) => {
            if (
                key.startsWith('DrawTool_') &&
                Array.isArray(L_.layers.layer[key])
            ) {
                L_.layers.layer[key].forEach((l) => {
                    try {
                        l.bringToFront()
                    } catch (err) {}
                })
            }
        })
    },
    refreshLayer: async function (
        layerObj,
        cb,
        skipOrderedBringToFront,
        stopLoops
    ) {
        // If it's a dynamic extent layer, just re-call its function
        if (
            L_._onSpecificLayerToggleSubscriptions[
                `dynamicextent_${layerObj.name}`
            ] != null
        ) {
            if (L_.layers.on[layerObj.name])
                L_._onSpecificLayerToggleSubscriptions[
                    `dynamicextent_${layerObj.name}`
                ].func(layerObj.name)

            if (typeof cb === 'function') cb()
            return true
        }

        // We need to find and remove all points on the map that belong to the layer
        // Not sure if there is a cleaner way of doing this
        for (var i = L_._layersOrdered.length - 1; i >= 0; i--) {
            if (
                L_.layers.data[L_._layersOrdered[i]] &&
                L_.layers.data[L_._layersOrdered[i]].type == 'vector' &&
                L_.layers.data[L_._layersOrdered[i]].name == layerObj.name
            ) {
                if (L_._layersBeingMade[layerObj.name] !== true) {
                    const wasOn = L_.layers.on[layerObj.name]

                    if (wasOn)
                        L_.toggleLayer(
                            L_.layers.data[layerObj.name],
                            skipOrderedBringToFront
                        ) // turn off if on

                    // fake on
                    L_.layers.on[layerObj.name] = true
                    await makeLayer(layerObj, true, null, null, null, stopLoops)
                    L_.addVisible(Map_, [layerObj.name])

                    // turn off if was off
                    if (wasOn) L_.layers.on[layerObj.name] = false
                    L_.toggleLayer(
                        L_.layers.data[layerObj.name],
                        skipOrderedBringToFront
                    ) // turn back on/off

                    L_.enforceVisibilityCutoffs()
                } else {
                    console.error(
                        `ERROR - refreshLayer: Cannot make layer ${layerObj.display_name}/${layerObj.name} as it's already being made!`
                    )
                    if (typeof cb === 'function') cb()
                    return false
                }
                if (typeof cb === 'function') cb()
                return true
            }
        }
    },
    setPlayerArrow(lng, lat, rot) {
        var playerMapArrowOffsets = [
            [0.06, 0],
            [-0.04, 0.04],
            [-0.02, 0],
            [-0.04, -0.04],
        ]
        var playerMapArrowPolygon = []

        if (Map_.map.hasLayer(Map_.player.arrow))
            Map_.map.removeLayer(Map_.player.arrow)
        var scalar = 512 / Math.pow(2, Map_.map.getZoom())
        var rotatedOffsets
        for (var i = 0; i < playerMapArrowOffsets.length; i++) {
            rotatedOffsets = F_.rotatePoint(
                {
                    x: playerMapArrowOffsets[i][0],
                    y: playerMapArrowOffsets[i][1],
                },
                [0, 0],
                -rot
            )
            playerMapArrowPolygon.push([
                lat + scalar * rotatedOffsets.x,
                lng + scalar * rotatedOffsets.y,
            ])
        }
        Map_.player.arrow = L.polygon(playerMapArrowPolygon, {
            color: 'lime',
            opacity: 1,
            lineJoin: 'miter',
            weight: 2,
        }).addTo(Map_.map)
    },
    setPlayerLookat(lng, lat) {
        if (Map_.map.hasLayer(Map_.player.lookat))
            Map_.map.removeLayer(Map_.player.lookat)
        if (lat && lng) {
            Map_.player.lookat = new L.circleMarker([lat, lng], {
                fillColor: 'lime',
                fillOpacity: 0.75,
                color: 'lime',
                opacity: 1,
                weight: 2,
            })
                .setRadius(5)
                .addTo(Map_.map)
        }
    },
    hidePlayer(hideArrow, hideLookat) {
        if (hideArrow !== false && Map_.map.hasLayer(Map_.player.arrow))
            Map_.map.removeLayer(Map_.player.arrow)
        if (hideLookat !== false && Map_.map.hasLayer(Map_.player.lookat))
            Map_.map.removeLayer(Map_.player.lookat)
    },
    getScreenDiagonalInMeters() {
        let bb = document.getElementById('map').getBoundingClientRect()
        let nwLatLng = Map_.map.containerPointToLatLng([0, 0])
        let seLatLng = Map_.map.containerPointToLatLng([bb.width, bb.height])
        return F_.lngLatDistBetween(
            nwLatLng.lng,
            nwLatLng.lat,
            seLatLng.lng,
            seLatLng.lat
        )
    },
    getCurrentTileXYZs() {
        const bounds = Map_.map.getBounds()
        const zoom = Map_.map.getZoom()

        const min = Map_.map
                .project(bounds.getNorthWest(), zoom)
                .divideBy(256)
                .floor(),
            max = Map_.map
                .project(bounds.getSouthEast(), zoom)
                .divideBy(256)
                .floor(),
            xyzs = [],
            mod = Math.pow(2, zoom)

        for (var i = min.x; i <= max.x; i++) {
            for (var j = min.y; j <= max.y; j++) {
                var x = ((i % mod) + mod) % mod
                var y = ((j % mod) + mod) % mod
                var coords = new L.Point(x, y)
                coords.z = zoom
                xyzs.push(coords)
            }
        }

        return xyzs
    },
    makeLayer: makeLayer,
    makeLayers: makeLayers,
    allLayersLoaded: allLayersLoaded,
}

//Takes an array of layer objects and makes them map layers
function makeLayers(layersObj) {
    //Make each layer (backwards to maintain draw order)
    for (var i = layersObj.length - 1; i >= 0; i--) {
        makeLayer(layersObj[i])
    }
}
//Takes the layer object and makes it a map layer
async function makeLayer(
    layerObj,
    evenIfOff,
    forceGeoJSON,
    id,
    forceMake,
    stopLoops
) {
    return new Promise(async (resolve, reject) => {
        const layerName = L_.asLayerUUID(layerObj.name)
        if (forceMake !== true && L_._layersBeingMade[layerName] === true) {
            console.error(
                `ERROR - makeLayer: Cannot make layer ${layerObj.display_name}/${layerObj.name} as it's already being made!`
            )
            resolve(false)
            return
        } else {
            L_._layersBeingMade[layerName] = true
        }
        //Decide what kind of layer it is
        //Headers do not need to be made
        if (layerObj.type != 'header') {
            //Simply call the appropriate function for each layer type
            switch (layerObj.type) {
                case 'vector':
                    await makeVectorLayer(
                        layerObj,
                        evenIfOff,
                        null,
                        forceGeoJSON
                    )
                    break
                case 'velocity':
                    await makeVelocityLayer(
                        layerObj,
                        evenIfOff,
                        null,
                        forceGeoJSON
                    )
                    break
                case 'tile':
                    makeTileLayer(layerObj)
                    break
                case 'vectortile':
                    makeVectorTileLayer(layerObj)
                    break
                case 'query':
                    await makeVectorLayer(layerObj, false, true, forceGeoJSON)
                    break
                case 'data':
                    makeDataLayer(layerObj)
                    break
                case 'image':
                    makeImageLayer(layerObj)
                    break
                case 'model':
                    //Globe only
                    makeModelLayer(layerObj)
                    break
                default:
                    console.warn('Unknown layer type: ' + layerObj.type)
            }
        }

        // release hold on layer
        L_._layersBeingMade[layerName] = false

        if (stopLoops !== true && layerObj.type === 'vector') {
            Filtering.updateGeoJSON(layerObj.name)
            Filtering.triggerFilter(layerObj.name)
        }
        resolve(true)
    })
}

//Default is onclick show full properties and onhover show 1st property
Map_.onEachFeatureDefault = onEachFeatureDefault
function onEachFeatureDefault(feature, layer) {
    const pv = L_.getLayersChosenNamePropVal(feature, layer)

    layer['useKeyAsName'] = Object.keys(pv)[0]
    if (
        layer.hasOwnProperty('options') &&
        layer.options.hasOwnProperty('layerName')
    ) {
        L_.layers.data[layer.options.layerName].useKeyAsName =
            layer['useKeyAsName']
    }

    if (typeof layer['useKeyAsName'] === 'string') {
        //Add a mouseover event to the layer
        layer.on('mouseover', function () {
            //Make it turn on CursorInfo and show name and value
            CursorInfo.update(pv, null, false)
        })
        //Add a mouseout event
        layer.on('mouseout', function () {
            //Make it turn off CursorInfo
            CursorInfo.hide()
        })
    }

    if (
        !(
            feature.style &&
            feature.style.hasOwnProperty('noclick') &&
            feature.style.noclick
        )
    ) {
        //Add a click event to send the data to the info tab
        layer.on('click', (e) => {
            featureDefaultClick(feature, layer, e)
        })
    }
}

Map_.featureDefaultClick = featureDefaultClick
function featureDefaultClick(feature, layer, e) {
    if (
        ToolController_.activeTool &&
        ToolController_.activeTool.disableLayerInteractions === true
    )
        return
    MetadataCapturer.populateMetadata(layer, () => {
        Kinds.use(
            L_.layers.data[layer.options.layerName].kind,
            Map_,
            feature,
            layer,
            layer.options.layerName,
            null,
            e
        )

        //update url
        if (layer != null && layer.hasOwnProperty('options')) {
            var keyAsName
            if (layer.hasOwnProperty('useKeyAsName')) {
                keyAsName = layer.feature.properties[layer.useKeyAsName]
            } else {
                keyAsName = layer.feature.properties[0]
            }
        }

        Viewer_.changeImages(feature, layer)

        //figure out how to construct searchStr in URL. For example: a ChemCam target can sometime
        //be searched by "target sol", or it can be searched by "sol target" depending on config file.
        var searchToolVars = L_.getToolVars('search')
        var searchfields = {}
        if (searchToolVars.hasOwnProperty('searchfields')) {
            for (var layerfield in searchToolVars.searchfields) {
                var fieldString = searchToolVars.searchfields[layerfield]
                fieldString = fieldString.split(')')
                for (var i = 0; i < fieldString.length; i++) {
                    fieldString[i] = fieldString[i].split('(')
                    var li = fieldString[i][0].lastIndexOf(' ')
                    if (li != -1) {
                        fieldString[i][0] = fieldString[i][0].substring(li + 1)
                    }
                }
                fieldString.pop()
                //0 is function, 1 is parameter
                searchfields[layerfield] = fieldString
            }
        }

        var str = ''
        if (searchfields.hasOwnProperty(layer.options.layerName)) {
            var sf = searchfields[layer.options.layerName] //sf for search field
            for (var i = 0; i < sf.length; i++) {
                str += sf[i][1]
                str += ' '
            }
        }
        str = str.substring(0, str.length - 1)

        var searchFieldTokens = str.split(' ')
        var searchStr

        if (searchFieldTokens.length == 2) {
            if (
                searchFieldTokens[0].toLowerCase() ==
                layer.useKeyAsName.toLowerCase()
            ) {
                searchStr = keyAsName + ' ' + layer.feature.properties.Sol
            } else {
                searchStr = layer.feature.properties.Sol + ' ' + keyAsName
            }
        }

        QueryURL.writeSearchURL([searchStr], layer.options.layerName)
    })
}

//Pretty much like makePointLayer but without the pointToLayer stuff
async function makeVectorLayer(
    layerObj,
    evenIfOff,
    useEmptyGeoJSON,
    forceGeoJSON
) {
    return new Promise((resolve, reject) => {
        if (forceGeoJSON) add(forceGeoJSON)
        else
            captureVector(
                layerObj,
                { evenIfOff: evenIfOff, useEmptyGeoJSON: useEmptyGeoJSON },
                add,
                (f) => {
                    Map_.map.on('moveend', f)
                    if (
                        layerObj.time?.enabled === true &&
                        layerObj.controlled !== true
                    )
                        L_.subscribeTimeChange(
                            `dynamicextent_${layerObj.name}`,
                            f
                        )
                    L_.subscribeOnSpecificLayerToggle(
                        `dynamicextent_${layerObj.name}`,
                        layerObj.name,
                        f
                    )
                }
            )

        function add(data, allowInvalid) {
            data = F_.parseIntoGeoJSON(data)

            let invalidGeoJSONTrace = gjv.valid(data, true)
            const allowableErrors = [`position must only contain numbers`]

            invalidGeoJSONTrace = invalidGeoJSONTrace.filter((t) => {
                if (typeof t !== 'string') return false
                for (let i = 0; i < allowableErrors.length; i++) {
                    if (t.toLowerCase().indexOf(allowableErrors[i]) != -1)
                        return false
                }
                return true
            })
            if (
                data == null ||
                data === 'off' ||
                (invalidGeoJSONTrace.length > 0 && allowInvalid !== true)
            ) {
                if (data != null && data != 'off') {
                    data = null
                    console.warn(
                        `ERROR: ${layerObj.display_name} has invalid GeoJSON!`
                    )
                }
                L_._layersLoaded[
                    L_._layersOrdered.indexOf(layerObj.name)
                ] = true
                L_.layers.layer[layerObj.name] = data == null ? null : false
                allLayersLoaded()
                resolve()
                return
            }

            layerObj.style = layerObj.style || {}
            layerObj.style.layerName = layerObj.name

            layerObj.style.opacity = L_.layers.opacity[layerObj.name]
            //layerObj.style.fillOpacity = L_.layers.opacity[layerObj.name]

            const vl = constructVectorLayer(
                data,
                layerObj,
                onEachFeatureDefault,
                Map_
            )
            L_.layers.attachments[layerObj.name] = vl.sublayers
            L_.layers.layer[layerObj.name] = vl.layer

            d3.selectAll('.' + F_.getSafeName(layerObj.name)).data(
                data.features
            )
            L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true

            allLayersLoaded()
            resolve()
        }
    })
}

//For vector velocity layers
async function makeVelocityLayer(
    layerObj,
    evenIfOff,
    useEmptyGeoJSON,
    forceGeoJSON
) {
    return new Promise((resolve, reject) => {
        if (forceGeoJSON) add(forceGeoJSON)
        else
            captureVector(
                layerObj,
                { evenIfOff: evenIfOff, useEmptyGeoJSON: useEmptyGeoJSON },
                add,
                (f) => {
                    Map_.map.on('moveend', f)
                    if (
                        layerObj.time?.enabled === true &&
                        layerObj.controlled !== true
                    )
                        L_.subscribeTimeChange(
                            `dynamicgeodataset_${layerObj.name}`,
                            f
                        )
                    L_.subscribeOnSpecificLayerToggle(
                        `dynamicgeodataset_${layerObj.name}`,
                        layerObj.name,
                        f
                    )
                }
            )

        function add(data, allowInvalid) {
            if (layerObj.type == 'velocity') {
                if (
                    layerObj.kind == 'streamlines' ||
                    'kind' in layerObj == false
                ) {
                    const defaultColors = [
                        'rgb(36,104, 180)',
                        'rgb(60,157, 194)',
                        'rgb(128,205,193 )',
                        'rgb(151,218,168 )',
                        'rgb(198,231,181)',
                        'rgb(238,247,217)',
                        'rgb(255,238,159)',
                        'rgb(252,217,125)',
                        'rgb(255,182,100)',
                        'rgb(252,150,75)',
                        'rgb(250,112,52)',
                        'rgb(245,64,32)',
                        'rgb(237,45,28)',
                        'rgb(220,24,32)',
                        'rgb(180,0,35)',
                    ]
                    let colorScale = ''
                    if (layerObj.variables?.streamlines?.colorScale) {
                        let colorConfig =
                            layerObj.variables?.streamlines?.colorScale
                        if (colorConfig.includes(',')) {
                            colorScale = colorConfig
                                .split('", "')
                                .map((item) => item.replace(/["]/g, ''))
                        } else if (colorConfig === 'DEFAULT') {
                            colorScale = defaultColors
                        } else {
                            // Assume we have a colormap name and look up the values
                            let reverse = false
                            if (colorConfig.endsWith('_r')) {
                                reverse = true
                                colorConfig = colorConfig.slice(0, -2)
                            }
                            colorScale = []
                            let colors = colormapData[colorConfig]?.colors
                            if (colors != null) {
                                colors
                                    .map((color) => {
                                        const r = Math.round(color[0] * 255)
                                        const g = Math.round(color[1] * 255)
                                        const b = Math.round(color[2] * 255)
                                        return `rgb(${r}, ${g}, ${b})`
                                    })
                                    .forEach((colorString) =>
                                        colorScale.push(colorString)
                                    )
                                if (reverse) {
                                    colorScale = colorScale.reverse()
                                }
                            } else {
                                colorScale = defaultColors
                            }
                        }
                    }
                    let velocityLayer = L.velocityLayer({
                        displayValues:
                            layerObj.variables?.streamlines?.displayValues,
                        displayOptions: {
                            position: layerObj.variables?.streamlines
                                ?.displayPosition
                                ? layerObj.variables?.streamlines
                                      ?.displayPosition
                                : 'bottomleft',
                            emptyString: '',
                        },
                        data: data,
                        minVelocity: layerObj.variables?.streamlines
                            ?.minVelocity
                            ? layerObj.variables.streamlines.minVelocity
                            : 0,
                        maxVelocity: layerObj.variables?.streamlines
                            ?.maxVelocity
                            ? layerObj.variables.streamlines.maxVelocity
                            : 15,
                        velocityScale: layerObj.variables?.streamlines
                            ?.velocityScale
                            ? layerObj.variables.streamlines.velocityScale
                            : 0.005,
                        particleAge: layerObj.variables?.streamlines
                            ?.particleAge
                            ? layerObj.variables.streamlines.particleAge
                            : 90,
                        lineWidth: layerObj.variables?.streamlines?.lineWidth
                            ? layerObj.variables.streamlines.lineWidth
                            : 1,
                        particleMultiplier: layerObj.variables?.streamlines
                            ?.particleMultiplier
                            ? layerObj.variables.streamlines.particleMultiplier
                            : 1 / 300,
                        frameRate: layerObj.variables?.streamlines?.frameRate
                            ? layerObj.variables.streamlines.frameRate
                            : 15,
                        colorScale: colorScale,
                    })
                    velocityLayer.setZIndex = function () {}
                    L_.layers.layer[layerObj.name] = velocityLayer
                } else if (layerObj.kind == 'particles') {
                    let points = []
                    if (data.features) {
                        data.features.forEach(function (feature) {
                            points.push([
                                feature.geometry.coordinates[1],
                                feature.geometry.coordinates[0],
                            ])
                        })
                    }
                    let options = {
                        angle: layerObj.variables?.particles?.angle
                            ? layerObj.variables?.particles?.angle
                            : 80,
                        width: layerObj.variables?.particles?.width
                            ? layerObj.variables?.particles?.width
                            : 1,
                        spacing: layerObj.variables?.particles?.spacing
                            ? layerObj.variables?.particles?.spacing
                            : 10,
                        length: layerObj.variables?.particles?.length
                            ? layerObj.variables?.particles?.length
                            : 4,
                        interval: layerObj.variables?.particles?.interval
                            ? layerObj.variables?.particles?.interval
                            : 10,
                        speed: layerObj.variables?.particles?.speed
                            ? layerObj.variables?.particles?.speed
                            : 0.1,
                        color: layerObj.style?.color
                            ? layerObj.style?.color
                            : 'Oxa6b3e9',
                    }
                    let rainLayer = L.rain(points, options)
                    rainLayer.setZIndex = function () {}
                    L_.layers.layer[layerObj.name] = rainLayer
                }
                L_._layersLoaded[
                    L_._layersOrdered.indexOf(layerObj.name)
                ] = true
            }
            allLayersLoaded()
            resolve()
        }
    })
}

async function makeTileLayer(layerObj) {
    let layerUrl = L_.getUrl(layerObj.type, layerObj.url, layerObj)

    let splitColonType
    const splitColonLayerUrl = layerObj.url.split(':')
    if (splitColonLayerUrl[1] != null) {
        let bandsParam = ''
        let b
        let resamplingParam = ''

        switch (splitColonLayerUrl[0]) {
            case 'stac-collection':
                splitColonType = splitColonLayerUrl[0]
                const splitParams = splitColonLayerUrl[1].split('?')

                // Bands
                bandsParam = ''
                b = layerObj.cogBands
                if (b != null) {
                    b.forEach((band) => {
                        if (band != null) bandsParam += `&bidx=${band}`
                    })
                }

                // Resampling
                resamplingParam = ''
                if (layerObj.cogResampling) {
                    resamplingParam = `&resampling=${layerObj.cogResampling}`
                }

                layerUrl = `${window.location.origin}${(
                    window.location.pathname || ''
                ).replace(/\/$/g, '')}/titilerpgstac/collections/${
                    splitParams[0]
                }/tiles/${
                    layerObj.tileMatrixSet || 'WebMercatorQuad'
                }/{z}/{x}/{y}?assets=asset${bandsParam}${resamplingParam}`
                layerObj.tileformat = 'wmts'
                break
            case 'COG':
                splitColonType = splitColonLayerUrl[0]
                // Bands
                bandsParam = ''
                b = layerObj.cogBands
                if (b != null) {
                    b.forEach((band) => {
                        if (band != null) bandsParam += `&bidx=${band}`
                    })
                }

                resamplingParam = ''
                if (layerObj.cogResampling) {
                    resamplingParam = `&resampling=${layerObj.cogResampling}`
                }

                layerUrl = `${window.location.origin}${(
                    window.location.pathname || ''
                ).replace(/\/$/g, '')}/titiler/cog/tiles/${
                    layerObj.tileMatrixSet || 'WebMercatorQuad'
                }/{z}/{x}/{y}.webp?url=${layerUrl}${bandsParam}${resamplingParam}`

            default:
                break
        }
    }

    let bb = null
    if (layerObj.hasOwnProperty('boundingBox')) {
        bb = L.latLngBounds(
            L.latLng(layerObj.boundingBox[3], layerObj.boundingBox[2]),
            L.latLng(layerObj.boundingBox[1], layerObj.boundingBox[0])
        )
    }
    layerUrl = await TimeControl.performTimeUrlReplacements(
        layerUrl,
        layerObj,
        null
    )

    let tileFormat = 'tms'
    // For backward compatibility with the .tms option
    if (typeof layerObj.tileformat === 'undefined') {
        tileFormat = typeof layerObj.tms === 'undefined' ? true : layerObj.tms
        tileFormat = tileFormat ? 'tms' : 'wmts'
    } else tileFormat = layerObj.tileformat

    L_.layers.layer[layerObj.name] = L.tileLayer.colorFilter(layerUrl, {
        minZoom: parseInt(layerObj.minZoom),
        maxZoom: parseInt(layerObj.maxZoom),
        maxNativeZoom: parseInt(layerObj.maxNativeZoom),
        tileFormat: tileFormat,
        tms: tileFormat === 'tms',
        splitColonType: splitColonType,
        //noWrap: true,
        continuousWorld: true,
        reuseTiles: true,
        bounds: bb,
        timeEnabled: layerObj.time != null && layerObj.time.enabled === true,
        time: typeof layerObj.time === 'undefined' ? '' : layerObj.time.end,
        compositeTile:
            typeof layerObj.time === 'undefined'
                ? false
                : layerObj.time.compositeTile || false,
        starttime:
            typeof layerObj.time === 'undefined' ? '' : layerObj.time.start,
        endtime: typeof layerObj.time === 'undefined' ? '' : layerObj.time.end,
        customTimes:
            typeof layerObj.time === 'undefined'
                ? null
                : layerObj.time.customTimes,
        cogTransform: layerObj.cogTransform,
        cogMin: layerObj.cogMin,
        currentCogMin: layerObj.currentCogMin,
        cogMax: layerObj.cogMax,
        currentCogMax: layerObj.currentCogMax,
        cogColormap: layerObj.cogColormap,
        variables: layerObj.variables || {},
    })

    L_.setLayerOpacity(layerObj.name, L_.layers.opacity[layerObj.name])

    L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true
    L_.layers.layer[layerObj.name].off('loading')
    L_.layers.layer[layerObj.name].on('loading', () => {
        L_.setGlobalLoading(layerObj.name)
    })
    L_.layers.layer[layerObj.name].off('load')
    L_.layers.layer[layerObj.name].on('load', () => {
        // Set default css filters for tile layer
        if (
            layerObj.style?.brightness != null &&
            L_.layers.filters[layerObj.name]?.brightness == null
        )
            L_.setLayerFilter(
                layerObj.name,
                'brightness',
                layerObj.style.brightness
            )
        if (
            layerObj.style?.contrast != null &&
            L_.layers.filters[layerObj.name]?.contrast == null
        )
            L_.setLayerFilter(
                layerObj.name,
                'contrast',
                layerObj.style.contrast
            )
        if (
            layerObj.style?.saturation != null &&
            L_.layers.filters[layerObj.name]?.saturation == null
        )
            L_.setLayerFilter(
                layerObj.name,
                'saturation',
                layerObj.style.saturation
            )
        if (
            layerObj.style?.blend != null &&
            L_.layers.filters[layerObj.name]?.blend == null
        )
            L_.setLayerFilter(
                layerObj.name,
                'mix-blend-mode',
                layerObj.style.blend
            )

        L_.setGlobalLoaded(layerObj.name)
    })
    allLayersLoaded()
}

function makeVectorTileLayer(layerObj) {
    let layerUrl = L_.getUrl(layerObj.type, layerObj.url, layerObj)

    let urlSplit = layerObj.url.split(':')

    if (urlSplit[0].toLowerCase() === 'geodatasets' && urlSplit[1] != null) {
        layerUrl =
            `${window.mmgisglobal.ROOT_PATH || ''}/api/geodatasets/get?layer=${
                urlSplit[1]
            }` + '&type=mvt&x={x}&y={y}&z={z}'
    }

    var bb = null
    if (layerObj.hasOwnProperty('boundingBox')) {
        bb = L.latLngBounds(
            L.latLng(layerObj.boundingBox[3], layerObj.boundingBox[2]),
            L.latLng(layerObj.boundingBox[1], layerObj.boundingBox[0])
        )
    }

    var clearHighlight = function () {
        for (let l of Object.keys(L_.layers.data)) {
            if (L_.layers.layer[l]) {
                var highlight = L_.layers.layer[l].highlight
                if (highlight) {
                    L_.layers.layer[l].resetFeatureStyle(highlight)
                }
                L_.layers.layer[l].highlight = null
            }
        }
    }
    var timedSelectTimeout = null
    var timedSelect = function (layer, layerName, e) {
        clearTimeout(timedSelectTimeout)
        timedSelectTimeout = setTimeout(
            (function (layer, layerName, e) {
                return function () {
                    let ell = { latlng: null }
                    if (e.latlng != null)
                        ell.latlng = JSON.parse(JSON.stringify(e.latlng))
                    MetadataCapturer.populateMetadata(layer, () => {
                        Kinds.use(
                            L_.layers.data[layerName].kind,
                            Map_,
                            L_.layers.layer[layerName].activeFeatures[0],
                            layer,
                            layerName,
                            null,
                            ell
                        )

                        ToolController_.getTool('InfoTool').use(
                            layer,
                            layerName,
                            L_.layers.layer[layerName].activeFeatures,
                            null,
                            null,
                            null,
                            ell
                        )
                        L_.layers.layer[layerName].activeFeatures = []
                    })
                }
            })(layer, layerName, e),
            100
        )
    }

    var vectorTileOptions = {
        layerName: layerObj.name,
        rendererFactory: L.svg.tile,
        vectorTileLayerStyles: layerObj.style.vtLayer || {},
        interactive: true,
        minZoom: layerObj.minZoom,
        maxZoom: layerObj.maxZoom,
        maxNativeZoom: layerObj.maxNativeZoom,
        getFeatureId: (function (vtId) {
            return function (f) {
                if (
                    f.properties.properties &&
                    typeof f.properties.properties === 'string'
                ) {
                    f.properties = JSON.parse(f.properties.properties)
                }
                return f.properties[vtId]
            }
        })(layerObj.style.vtId),
    }

    L_.layers.layer[layerObj.name] = L.vectorGrid
        .protobuf(layerUrl, vectorTileOptions)
        .on('click', function (e, b, x) {
            let layerName = e.target.options.layerName
            let vtId = L_.layers.layer[layerName].vtId
            clearHighlight()
            L_.layers.layer[layerName].highlight = e.layer.properties[vtId]

            L_.layers.layer[layerName].setFeatureStyle(
                L_.layers.layer[layerName].highlight,
                {
                    weight: 2,
                    color: 'red',
                    opacity: 1,
                    fillColor: 'red',
                    fill: true,
                    radius: 4,
                    fillOpacity: 1,
                }
            )
            L_.layers.layer[layerName].activeFeatures =
                L_.layers.layer[layerName].activeFeatures || []
            L_.layers.layer[layerName].activeFeatures.push({
                type: 'Feature',
                properties: e.layer.properties,
                geometry: {},
            })

            Map_.activeLayer = e.layer
            if (Map_.activeLayer) L_.Map_._justSetActiveLayer = true

            let p = e.sourceTarget._point

            if (p) {
                for (var i in e.layer._renderer._features) {
                    if (
                        e.layer._renderer._features[i].feature._pxBounds.min
                            .x <= p.x &&
                        e.layer._renderer._features[i].feature._pxBounds.max
                            .x >= p.x &&
                        e.layer._renderer._features[i].feature._pxBounds.min
                            .y <= p.y &&
                        e.layer._renderer._features[i].feature._pxBounds.max
                            .y >= p.y &&
                        e.layer._renderer._features[i].feature.properties[
                            vtId
                        ] != e.layer.properties[vtId]
                    ) {
                        L_.layers.layer[layerName].activeFeatures.push({
                            type: 'Feature',
                            properties:
                                e.layer._renderer._features[i].feature
                                    .properties,
                            geometry: {},
                        })
                    }
                }
            }

            timedSelect(e.layer, layerName, e)

            L.DomEvent.stop(e)
        })
        .on(
            'mouseover',
            (function (vtKey) {
                return function (e, a, b, c) {
                    if (vtKey != null)
                        CursorInfo.update(
                            vtKey + ': ' + e.layer.properties[vtKey],
                            null,
                            false
                        )
                }
            })(layerObj.style.vtKey)
        )
        .on('mouseout', function () {
            CursorInfo.hide()
        })

    L_.layers.layer[layerObj.name].vtId = layerObj.style.vtId
    L_.layers.layer[layerObj.name].vtKey = layerObj.style.vtKey

    L_.setLayerOpacity(layerObj.name, L_.layers.opacity[layerObj.name])

    L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true
    allLayersLoaded()
}

function makeModelLayer(layerObj) {
    L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true
    allLayersLoaded()
}

function makeDataLayer(layerObj) {
    let layerUrl = L_.getUrl(layerObj.type, layerObj.demtileurl, layerObj)

    let bb = null
    if (layerObj.hasOwnProperty('boundingBox')) {
        bb = L.latLngBounds(
            L.latLng(layerObj.boundingBox[3], layerObj.boundingBox[2]),
            L.latLng(layerObj.boundingBox[1], layerObj.boundingBox[0])
        )
    }

    const shader = F_.getIn(layerObj, 'variables.shader') || {}
    const shaderType = shader.type || 'image'

    var uniforms = {}
    for (let i = 0; i < DataShaders[shaderType].settings.length; i++) {
        uniforms[DataShaders[shaderType].settings[i].parameter] =
            DataShaders[shaderType].settings[i].value
    }

    L_.layers.layer[layerObj.name] = L.tileLayer.gl({
        options: {
            tms: true,
            bounds: bb,
        },
        fragmentShader: DataShaders[shaderType].frag,
        tileUrls: [layerUrl],
        pixelPerfect: true,
        uniforms: uniforms,
    })

    if (DataShaders[shaderType].attachImmediateEvents) {
        DataShaders[shaderType].attachImmediateEvents(layerObj.name, shader)
    }

    L_.setLayerOpacity(layerObj.name, L_.layers.opacity[layerObj.name])

    L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true
    allLayersLoaded()
}

function makeImageLayer(layerObj) {
    let layerUrl = L_.getUrl(layerObj.type, layerObj.url, layerObj)
    if (!F_.isUrlAbsolute(layerUrl)) {
        layerUrl = `${window.location.origin}${(
            window.location.pathname || ''
        ).replace(/\/$/g, '')}/${layerUrl}`
    }

    let bb = null
    if (layerObj.hasOwnProperty('boundingBox')) {
        bb = L.latLngBounds(
            L.latLng(layerObj.boundingBox[3], layerObj.boundingBox[2]),
            L.latLng(layerObj.boundingBox[1], layerObj.boundingBox[0])
        )
    }

    const cogColormap = F_.getIn(L_.layers.data[layerObj.name], 'cogColormap')

    parseGeoraster(layerUrl)
        .then((georaster) => {
            let pixelValuesToColorFn = null
            if (
                F_.getIn(
                    L_.layers.data[layerObj.name],
                    'variables.hideNoDataValue'
                ) === true
            ) {
                pixelValuesToColorFn = (values) => {
                    // https://github.com/GeoTIFF/georaster-layer-for-leaflet/issues/16
                    return values[0] === georaster.noDataValue
                        ? null
                        : `rgb(${values[0]},${values[1]},${values[2]})`
                }
            }

            const imageInfo = F_.getIn(
                L_.layers.data[layerObj.name],
                'variables.image'
            )

            const hideNoDataValue = F_.getIn(
                L_.layers.data[layerObj.name],
                'variables.hideNoDataValue'
            )

            let min = null
            let max = null
            if (georaster.numberOfRasters === 1) {
                min = layerObj.cogMin
                max = layerObj.cogMax

                if (
                    isNaN(parseFloat(layerObj.cogMin)) ||
                    isNaN(parseFloat(layerObj.cogMax))
                ) {
                    // Try to get the min and max values using gdal if the user did not input min/max in the layer config
                    $.ajax({
                        type: calls.getminmax.type,
                        url: calls.getminmax.url,
                        data: {
                            type: 'minmax',
                            path: calls.getprofile.pathprefix + layerUrl,
                            bands: '[1]', // Assume the geotiff images only have a single band
                        },
                        async: false,
                        success: function (data) {
                            if (
                                data &&
                                data[0] &&
                                data[0].band &&
                                data[0].band === 1
                            ) {
                                if (isNaN(parseFloat(layerObj.cogMin))) {
                                    min = data[0].min
                                    layerObj.cogMin = min
                                }
                                if (isNaN(parseFloat(layerObj.cogMax))) {
                                    max = data[0].max
                                    layerObj.cogMax = max
                                }
                            }
                        },
                        error: function (request, status, error) {
                            console.warn(
                                `Failed to get gdal minmax info for ${layerObj.name}`,
                                request,
                                status,
                                error
                            )
                        },
                    })
                }

                // FIXME A lot of this code is duplicated in LayersTool so find some way to consolidate them as functions
                var range = max - min
                let colormap = null
                let reverse = false
                if (
                    layerObj.cogTransform === true &&
                    'cogColormap' in layerObj
                ) {
                    colormap = layerObj.cogColormap
                    // TiTiler colormap variables are all lower case so we need to format them correctly for js-colormaps
                    if (colormap.toLowerCase().endsWith('_r')) {
                        colormap = colormap.substring(0, colormap.length - 2)
                        reverse = true
                    }

                    let index = Object.keys(colormapData).findIndex((v) => {
                        return v.toLowerCase() === colormap.toLowerCase()
                    })

                    if (index > -1) {
                        colormap = Object.keys(colormapData)[index]
                    } else {
                        colormap = 'binary' // Give it the default value
                    }
                } else {
                    colormap = 'binary' // Give it the default value
                }

                pixelValuesToColorFn = (values) => {
                    var pixelValue = values[0] // single band
                    // don't return a color
                    if (
                        georaster.noDataValue != null &&
                        georaster.noDataValue === pixelValue
                    ) {
                        if (hideNoDataValue) {
                            return null
                        }

                        // Handle the case where we do not want to hide noDataValue
                        return [0, 0, 0]
                    }

                    // scale from 0 - 1
                    var scaledPixelValue = (pixelValue - min) / range
                    if (!(scaledPixelValue >= 0 && scaledPixelValue <= 1)) {
                        if (imageInfo && imageInfo.fillMinMax) {
                            if (scaledPixelValue <= 0) {
                                scaledPixelValue = 0
                            } else if (scaledPixelValue >= 1.0) {
                                scaledPixelValue = 1
                            }
                        } else {
                            return null
                        }
                    }

                    return evaluate_cmap(
                        scaledPixelValue,
                        colormap || IMAGE_DEFAULT_COLOR_RAMP,
                        reverse
                    )
                }
            }

            L_.layers.layer[layerObj.name] = new GeoRasterLayer({
                georaster: georaster,
                resolution: 256,
                opacity: 1.0,
                pixelValuesToColorFn: pixelValuesToColorFn,
            })

            L_.layers.layer[layerObj.name].clearCache()

            L_.layers.layer[layerObj.name].setZIndex(
                L_._layersOrdered.length +
                    1 -
                    L_._layersOrdered.indexOf(layerObj.name)
            )

            L_.setLayerOpacity(layerObj.name, L_.layers.opacity[layerObj.name])

            L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true
            allLayersLoaded()
        })
        .catch((e) => {
            console.warn(`WARNING - Unable to load image: ${layerUrl}`)

            L_._layersLoaded[L_._layersOrdered.indexOf(layerObj.name)] = true
            L_.layers.layer[layerObj.name] = null
            allLayersLoaded()
        })
}

//Because some layers load faster than others, check to see if
// all our layers were loaded before moving on
function allLayersLoaded() {
    if (!Map_.allLayersLoadedPassed) {
        //Only continues if all layers have been loaded
        for (var i = 0; i < L_._layersLoaded.length; i++) {
            if (L_._layersLoaded[i] == false) {
                return
            }
        }
        Map_.allLayersLoadedPassed = true

        //Then do these
        essenceFina()
        L_.addVisible(Map_)
        L_.enforceVisibilityCutoffs()

        ToolController_.finalizeTools()

        L_.loaded()
        //OTHER TEMPORARY TEST STUFF THINGS

        // Turn on legend if displayOnStart is true
        if ('LegendTool' in ToolController_.toolModules) {
            if (
                ToolController_.toolModules['LegendTool'].displayOnStart == true
            ) {
                ToolController_.toolModules['LegendTool'].make(
                    'toolContentSeparated_Legend'
                )
                ToolController_.activeSeparatedTools.push('LegendTool')
                let _event = new CustomEvent('toggleSeparatedTool', {
                    detail: {
                        toggledToolName: 'LegendTool',
                        visible: true,
                    },
                })
                document.dispatchEvent(_event)
            }
        }
    }
}

function buildToolBar() {
    d3.select('#mapToolBar').html('')

    Map_.toolBar = d3
        .select('#mapToolBar')
        .append('div')
        .attr('class', 'row childpointerevents')
        .style('height', '100%')
    Map_.toolBar
        .append('div')
        .attr('id', 'scaleBarBounds')
        .style('width', '270px')
        .style('height', '36px')
        .append('svg')
        .attr('id', 'scaleBar')
        .attr('width', '270px')
        .attr('height', '36px')
}

function clearOnMapClick(event) {
    if (Map_._justSetActiveLayer) {
        Map_._justSetActiveLayer = false
        return
    }
    // Skip if there is no actively selected feature
    if (!Map_.activeLayer) {
        return
    }

    if ('latlng' in event) {
        // Position of clicked element
        const latlng = event.latlng

        let found = false
        // For all MMGIS layers
        for (let key in L_.layers.layer) {
            if (L_.layers.layer[key] === false || L_.layers.layer[key] == null)
                continue
            let layers

            // Layers can be a LayerGroup or an array of LayerGroup
            if ('getLayers' in L_.layers.layer[key]) {
                layers = L_.layers.layer[key].getLayers()
            }

            if (Array.isArray(L_.layers.layer[key])) {
                layers = L_.layers.layer[key]
            }

            for (let k in layers) {
                const layer = layers[k]
                if (!layer) continue
                if ('getLayers' in layer) {
                    const _layer = layer.getLayers()
                    for (let x in _layer) {
                        found = checkBounds(_layer[x])
                        // We should bubble down further for layers that have no fill, as it is possible
                        // for there to be layers with features under the transparent fill
                        if (found) {
                            if (layer.options.fill) {
                                break
                            } else {
                                found = false
                            }
                        }
                    }
                } else {
                    found = checkBounds(layer)
                    if (found) {
                        // We should bubble down further for layers that have no fill, as it is possible
                        // for there to be layers with features under the transparent fill
                        if (layer.options.fill) {
                            break
                        } else {
                            found = false
                        }
                    }
                }

                if (found) break
            }

            if (found) {
                // If a clicked feature is found, break out early because MMGIS can only select
                // a single feature at a time (i.e. no group select)
                break
            }

            function checkBounds(layer) {
                if (
                    layer.feature &&
                    layer.feature.geometry.type.toLowerCase() === 'polygon'
                ) {
                    if (
                        L.leafletPip.pointInLayer(
                            [latlng.lng, latlng.lat],
                            layer
                        ).length > 0
                    )
                        return true
                } else if ('getBounds' in layer) {
                    // Use the pixel bounds because longitude/latitude conversions for bounds
                    // may be odd in the case of polar projections
                    if (
                        layer._pxBounds &&
                        layer._pxBounds.contains(event.layerPoint)
                    ) {
                        return true
                    }
                } else if ('getLatLng' in layer) {
                    // A latlng is a latlng, regardless of the projection type
                    // WARNING: This is imperfect because the click latlng and marker center latlng
                    // can differ but still intersect
                    if (layer.getLatLng().equals(latlng)) {
                        return true
                    }
                }
                return false
            }
        }

        // If no feature was selected by this click event, clear the currently selected item
        if (!found) {
            L_.setActiveFeature(null)
        }
    }
}

export default Map_
