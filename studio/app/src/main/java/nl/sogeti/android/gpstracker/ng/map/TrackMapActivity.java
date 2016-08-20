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
package nl.sogeti.android.gpstracker.ng.map;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import nl.sogeti.android.gpstracker.integration.ContentConstants;
import nl.sogeti.android.gpstracker.ng.tracks.TracksActivity;
import nl.sogeti.android.gpstracker.v2.R;
import nl.sogeti.android.gpstracker.v2.databinding.ActivityTrackMapBinding;


public class TrackMapActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TRACK_URI = "KEY_SELECTED_TRACK_URI";
    private static final int ITEM_ID_LIST_TRACKS = 3;
    private static final int ITEM_ID_EDIT_TRACK = 4;
    private static final int REQUEST_CODE_TRACK_SELECTION = 234;

    private TrackViewModel selectedTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityTrackMapBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_track_map);
        TrackMapFragment mapFragment = (TrackMapFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_map);
        selectedTrack = mapFragment.getTrackViewModel();
        binding.setTrack(selectedTrack);
        setSupportActionBar(binding.toolbar);
        binding.toolbar.bringToFront();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, ITEM_ID_LIST_TRACKS, Menu.NONE, "List tracks");
        menu.add(Menu.NONE, ITEM_ID_EDIT_TRACK, Menu.NONE, R.string.activity_track_map_edit);
        MenuItem menuItem = menu.findItem(ITEM_ID_EDIT_TRACK);
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        Drawable drawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_mode_edit_black_24dp, null);
        drawable = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.primary_light));
        menuItem.setIcon(drawable);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(ITEM_ID_EDIT_TRACK).setEnabled(selectedTrack.uri.get() != null);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean consumed;
        if (item.getItemId() == ITEM_ID_EDIT_TRACK) {
            showTrackTitleDialog();
            consumed = true;
        } else if (item.getItemId() == ITEM_ID_LIST_TRACKS) {
            Intent intent = new Intent(this, TracksActivity.class);
            startActivityForResult(intent, REQUEST_CODE_TRACK_SELECTION);
            consumed = true;
        } else {
            consumed = super.onOptionsItemSelected(item);
        }

        return consumed;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_TRACK_URI, selectedTrack.uri.get());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_TRACK_SELECTION && resultCode == RESULT_OK) {
            Uri trackUri = data.getParcelableExtra(ContentConstants.Tracks.TRACKS);
            selectedTrack.uri.set(trackUri);
        }
    }

    private void showTrackTitleDialog() {
        final Uri trackUri = selectedTrack.uri.get();
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_edittext, null, false);
        final EditText nameField = (EditText) view.findViewById(R.id.dialog_rename_edittext);
        nameField.setText(selectedTrack.name.get());
        nameField.setSelection(0, nameField.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.activity_track_map_rename_title))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String userInput = nameField.getText().toString();
                        TrackPresenter.updateName(TrackMapActivity.this, trackUri, userInput);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setView(view)
                .show();
    }
}