package com.elyeproj.loaderviewlibrary;

import android.graphics.Paint;

/*
 * Copyright 2016 Elye Project
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

//
// TODO Adding these files directly, since the library available via maven is based on AndroidX, and
// TODO we have yet to migrate. Once we do, these files and this package can be removed and the
// TODO library referenced directly from build.gradle.
//  See: https://github.com/elye/loaderviewlibrary for the github repo
//

interface LoaderView {
    void setRectColor(Paint rectPaint);

    void invalidate();

    boolean valueSet();
}
