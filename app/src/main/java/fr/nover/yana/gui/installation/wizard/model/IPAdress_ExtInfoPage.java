/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nover.yana.gui.installation.wizard.model;

import fr.nover.yana.gui.installation.wizard.ui.IPAdress_ExtInfoFragment;

import android.support.v4.app.Fragment;
import android.text.TextUtils;

import java.util.ArrayList;

/**
 * A page asking for a name and an email.
 */
public class IPAdress_ExtInfoPage extends Page {
    public static final String IPADRESS_DATA_KEY = "Lien interne";
    public static final String SSID_DATA_KEY = "Nom du r�seau local";
    public static final String IPADRESS_EXT_DATA_KEY = "Lien externe";

    public IPAdress_ExtInfoPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return IPAdress_ExtInfoFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem(IPADRESS_DATA_KEY, mData.getString(IPADRESS_DATA_KEY), getKey(), -1));
        dest.add(new ReviewItem(SSID_DATA_KEY, mData.getString(SSID_DATA_KEY), getKey(), -1));
        dest.add(new ReviewItem(IPADRESS_EXT_DATA_KEY, mData.getString(IPADRESS_EXT_DATA_KEY), getKey(), -1));
    }

    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(IPADRESS_DATA_KEY));
    }

}
