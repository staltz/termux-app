/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.termux.opengl;

public final class MeshData {
    // The grid lines on the floor are rendered procedurally and large polygons cause floating point
    // precision problems on some architectures. So we split the floor into 4 quadrants.
    public static final float[] FLOOR_COORDS = new float[] {
        // +X, +Z quadrant
        200, 0, 0,
        0, 0, 0,
        0, 0, 200,
        200, 0, 0,
        0, 0, 200,
        200, 0, 200,

        // -X, +Z quadrant
        0, 0, 0,
        -200, 0, 0,
        -200, 0, 200,
        0, 0, 0,
        -200, 0, 200,
        0, 0, 200,

        // +X, -Z quadrant
        200, 0, -200,
        0, 0, -200,
        0, 0, 0,
        200, 0, -200,
        0, 0, 0,
        200, 0, 0,

        // -X, -Z quadrant
        0, 0, -200,
        -200, 0, -200,
        -200, 0, 0,
        0, 0, -200,
        -200, 0, 0,
        0, 0, 0,
    };

    public static final float[] FLOOR_NORMALS = new float[] {
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
    };

    public static final float[] FLOOR_COLORS = new float[] {
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
        0.1647f, 0.0470f, 0.2549f, 1.0f,
    };

    public static final float[] SCREEN_COORDS = new float[] {
        -2.5f, 3.0f, 0.0f,
        -2.5f, -2.0f, 0.0f,
        2.5f, 3.0f, 0.0f,
        -2.5f, -2.0f, 0.0f,
        2.5f, -2.0f, 0.0f,
        2.5f, 3.0f, 0.0f,
    };

    public static final float[] SCREEN_NORMALS = new float[] {
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
    };

    public static final float[] SCREEN_COLORS = new float[] {
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
    };

    public static final float[] SCREEN_TEXTURE_COORDS = new float[] {
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
    };

    public static final float[] CLOCK_COORDS = new float[] {
        -1.25f, 3.6666f, 0.0f,
        -1.25f, 3.3333f, 0.0f,
        1.25f, 3.6666f, 0.0f,
        -1.25f, 3.3333f, 0.0f,
        1.25f, 3.3333f, 0.0f,
        1.25f, 3.6666f, 0.0f,
    };

    public static final float[] CLOCK_NORMALS = new float[] {
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
        0.0f, 0.0f, 1.0f,
    };

    public static final float[] CLOCK_COLORS = new float[] {
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
        1.0f, 1.0f, 1.0f, 1.0f,
    };

    public static final float[] CLOCK_TEXTURE_COORDS = new float[] {
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f,
        1.0f, 0.0f,
    };
}
