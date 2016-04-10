/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: rene
 ** Copyright: (c) 2016 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced
 ** Distributed Software Engineering |  or transmitted in any form or by any
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the
 ** 4131 NJ Vianen                   |  purpose, without the express written
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.map;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.databinding.Observable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Pair;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;

import nl.sogeti.android.gpstracker.BaseTrackAdapter;
import nl.sogeti.android.gpstracker.integration.ContentConstants;
import nl.sogeti.android.gpstracker.integration.ServiceManager;
import nl.sogeti.android.log.Log;

public class TrackAdaptor extends BaseTrackAdapter {

    private TrackViewModel viewModel;
    private ContentObserver observer;
    private TrackUriChangeListener uriChangeListener = new TrackUriChangeListener();
    private boolean isReading;

    public TrackAdaptor(TrackViewModel track) {
        this.viewModel = track;
    }

    public void start(Context context) {
        super.start(context, false);
        isReading = false;
        readUri();
        viewModel.uri.addOnPropertyChangedCallback(uriChangeListener);
    }

    public void stop() {
        viewModel.uri.removeOnPropertyChangedCallback(uriChangeListener);
        startObserver();
        super.stop();
    }

    private void startObserver() {
        if (observer != null) {
            getContext().getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
    }

    private void stopObserver(Uri trackUri) {
        startObserver();
        observer = new TrackObserver();
        getContext().getContentResolver().registerContentObserver(trackUri, true, observer);
    }

    private void readUri() {
        Uri trackUri = viewModel.uri.get();
        if (trackUri != null) {
            stopObserver(trackUri);
            new TrackReader(trackUri, viewModel).execute();
        }
    }

    @Override
    protected void didConnectService(ServiceManager serviceManager) {
    }

    @Override
    public void didChangeLoggingState(Intent intent) {
    }

    public static void updateName(Context context, Uri trackUri, String name) {
        ContentValues values = new ContentValues();
        values.put(ContentConstants.TracksColumns.NAME, name);
        context.getContentResolver().update(trackUri,values, null,null);
    }

    private class TrackObserver extends ContentObserver {
        public TrackObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d(this, "Uri changed " + uri + "(currently reading:" + isReading + ")");
            if (!isReading) {
                new TrackReader(viewModel.uri.get(), viewModel).execute();
            }
        }
    }

    private class TrackUriChangeListener extends Observable.OnPropertyChangedCallback {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            Uri trackUri = viewModel.uri.get();
            if (trackUri == null) {
                startObserver();
                viewModel.setDefaultName();
                viewModel.waypoints.set(null);
            } else {
                readUri();
            }
        }
    }

    private class TrackReader extends AsyncTask<Void, Void, LatLng[][]> implements ResultHandler {

        final ArrayList<ArrayList<LatLng>> collectedWaypoints = new ArrayList<>();
        private final Uri trackUri;
        private final TrackViewModel viewModel;
        private LatLng latLngFirst;
        private LatLng latLngLast;

        TrackReader(final Uri trackUri, final TrackViewModel viewModel) {
            this.trackUri = trackUri;
            this.viewModel = viewModel;
        }

        @Override
        public void addTrack(String name) {
            viewModel.name.set(name);
        }

        @Override
        public void addSegment() {
            collectedWaypoints.add(new ArrayList<LatLng>());
        }

        @Override
        public Pair<String, String[]> getWaypointSelection() {
            return null;
        }


        @Override
        public void addWaypoint(LatLng latLng, long millisecondsTime) {
            if (latLngFirst == null) {
                latLngFirst = latLng;
            }
            latLngLast = latLng;
            collectedWaypoints.get(collectedWaypoints.size() - 1).add(latLng);
        }

        @Override
        protected void onPreExecute() {
            isReading = true;
        }

        @Override
        protected LatLng[][] doInBackground(Void[] params) {
            readTrack(trackUri, this);
            LatLng[][] segmentedWaypoints = new LatLng[collectedWaypoints.size()][];
            for (int i = 0; i < collectedWaypoints.size(); i++) {
                ArrayList<LatLng> var = collectedWaypoints.get(i);
                segmentedWaypoints[i] = var.toArray(new LatLng[var.size()]);
            }

            return segmentedWaypoints;
        }

        @Override
        protected void onPostExecute(LatLng[][] segmentedWaypoints) {
            viewModel.bounds.set(new LatLngBounds(latLngFirst, latLngFirst).including(latLngLast));
            viewModel.waypoints.set(segmentedWaypoints);
            isReading = false;
        }
    }
}