package org.peimari.gleafletexample.client;

import org.peimari.gleaflet.client.AbstractPath;
import org.peimari.gleaflet.client.Circle;
import org.peimari.gleaflet.client.ClickListener;
import org.peimari.gleaflet.client.FeatureGroup;
import org.peimari.gleaflet.client.GeoJSON;
import org.peimari.gleaflet.client.ILayer;
import org.peimari.gleaflet.client.LatLng;
import org.peimari.gleaflet.client.LatLngBounds;
import org.peimari.gleaflet.client.MapWidget;
import org.peimari.gleaflet.client.MouseEvent;
import org.peimari.gleaflet.client.PathOptions;
import org.peimari.gleaflet.client.Polygon;
import org.peimari.gleaflet.client.Polyline;
import org.peimari.gleaflet.client.Rectangle;
import org.peimari.gleaflet.client.TileLayer;
import org.peimari.gleaflet.client.TileLayerOptions;
import org.peimari.gleaflet.client.draw.Draw;
import org.peimari.gleaflet.client.draw.DrawControlOptions;
import org.peimari.gleaflet.client.draw.LayerCreatedEvent;
import org.peimari.gleaflet.client.draw.LayerCreatedListener;
import org.peimari.gleaflet.client.draw.LayerType;
import org.peimari.gleaflet.client.resources.LeafletResourceInjector;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class App implements EntryPoint {

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		LeafletResourceInjector.ensureInjected();

		final MapWidget mapWidget = new MapWidget();

		RootPanel.get().add(mapWidget);

		mapWidget.getMap().setView(LatLng.create(61, 22), 5);

		TileLayerOptions tileOptions = TileLayerOptions.create();
		tileOptions.setSubDomains("a", "b", "c");
		TileLayer layer = TileLayer.create(
				"http://{s}.tile.osm.org/{z}/{x}/{y}.png", tileOptions);
		mapWidget.getMap().addLayer(layer);

		final CheckBox checkBox = new CheckBox("Toggle click listener");
		checkBox.addClickHandler(new ClickHandler() {

			ClickListener leafletClickListener = new ClickListener() {

				public void onClick(MouseEvent event) {
					LatLng latLng = event.getLatLng();
					Window.alert("Clicked at " + latLng);

				}
			};

			public void onClick(ClickEvent event) {
				if (checkBox.getValue()) {
					mapWidget.getMap().addClickListener(leafletClickListener);
				} else {
					mapWidget.getMap().removeClickListeners();
				}

			}
		});

		RootPanel.get().add(checkBox);

		// Leaflet.Draw

		final GeoJSON featureGroup = GeoJSON.create();

		PathOptions pathOptions = PathOptions.create();
		pathOptions.setColor("red");
		Circle circle = Circle.create(LatLng.create(61, 22), 50000d,
				pathOptions);
		featureGroup.addLayer(circle);

		mapWidget.getMap().addLayer(featureGroup);

		DrawControlOptions drawOptions = DrawControlOptions.create();
		drawOptions.setEditableFeatureGroup(featureGroup);
		Draw drawControl = Draw.create(drawOptions);

		mapWidget.getMap().addControl(drawControl);

		mapWidget.getMap().addLayerCreatedListener(new LayerCreatedListener() {

			public void onCreated(LayerCreatedEvent event) {
				LayerType type = event.getLayerType();
				/* type specific actions... */
				switch (type) {
				case marker:
					featureGroup.addLayer(event.getLayer());
					return;

				case circle:
					Circle c = (Circle) event.getLayer();
					LatLng latLng = c.getLatLng();
					double radius = c.getRadius();
					Window.alert("Created circle at " + latLng + " with "
							+ radius + "m radius. {"
							+ new JSONObject(c.toGeoJSON()).toString() + "}");
					break;
				case polygon:
					Polygon p = (Polygon) event.getLayer();
					LatLng[] latlngs = p.getLatLngs();
					Window.alert("Created polygon: " + p.getRawLatLngs());
					break;
				case polyline:
					Polyline pl = (Polyline) event.getLayer();
					LatLng[] latLngs2 = pl.getLatLngs();
					Window.alert("Created polyline: " + pl.getRawLatLngs());
					break;
				case rectangle:
					Rectangle r = (Rectangle) event.getLayer();
					LatLng[] latLngs3 = r.getLatLngs();
					LatLngBounds bounds = r.getBounds();
					Window.alert("Created rectangle: " + r.getRawLatLngs());
					break;
				default:
					break;
				}
				PathOptions newPathOptions = PathOptions.create();
				newPathOptions.setColor("green");
				AbstractPath path = (AbstractPath) event.getLayer();
				path.setStyle(newPathOptions);
				path.redraw();
				featureGroup.addLayer(path);

			}
		});

		Button button = new Button("Save layer to LocalStorage");

		button.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {

				String key = Window.prompt("Name for your layer?",
						"my saved map");

				ILayer[] layers = featureGroup.getLayers();

				JsArray<JavaScriptObject> geojsFeatures = JsArray.createArray()
						.cast();
				for (ILayer iLayer : layers) {
					AbstractPath p = (AbstractPath) iLayer;
					geojsFeatures.push(p.toGeoJSON());
				}

				String geojsonstr = new JSONArray(geojsFeatures).toString();

				Storage.getLocalStorageIfSupported().setItem(key, geojsonstr);

			}
		});

		RootPanel.get().add(button);

		button = new Button("Load saved map");

		button.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				String key = Window.prompt("Name for your layer?",
						"my saved map");

				try {
					String geojsonstr = Storage.getLocalStorageIfSupported()
							.getItem(key);
					System.out.println(geojsonstr);
					JsArray<JavaScriptObject> features = JsonUtils
							.safeEval(geojsonstr);
					featureGroup.clearLayers();

					for (int i = 0; i < features.length(); i++) {
						featureGroup.addData(features.get(i));
					}

					Window.alert("Loaded " + features.length() + " feature(s).");

				} catch (Exception e) {
					Window.alert("Failed to load features");
				}

			}
		});

		RootPanel.get().add(button);

	}
}
