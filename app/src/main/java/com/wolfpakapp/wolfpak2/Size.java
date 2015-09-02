/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wolfpakapp.wolfpak2;

import android.graphics.Point;

/**
 * This is part of the AOSP implementation of the android.util.Size class. This is necessary for
 * backwards compatibility for versions of Android before API 21 (Lollipop).
 */
public class Size {
    private final int width;
    private final int height;

    public Size(Point point) {
        this.width = point.x;
        this.height = point.y;
    }

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return width + " x " + height;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Size)) {
            return false;
        }

        Size otherSize = (Size) other;
        return otherSize.width == this.width && otherSize.height == this.height;
    }
}
