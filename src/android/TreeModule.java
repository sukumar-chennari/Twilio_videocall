package cordova.plugin.videocall.TreeModule;/*
 * Copyright (C) 2019 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.twilio.video.LogLevel;
import com.twilio.video.Video;

import cordova.plugin.videocall.ApplicationScope.ApplicationScope;
import cordova.plugin.videocall.BuildConfig.BuildConfig;
import cordova.plugin.videocall.CrashlyticsTreeRanger.CrashlyticsTreeRanger;
import cordova.plugin.videocall.DebugTree.DebugTree;
import cordova.plugin.videocall.ReleaseTree.ReleaseTree;
import dagger.Module;
import dagger.Provides;
import src.cordova.plugin.videocall.BuildConfigUtils.BuildConfigUtilsKt;
import timber.log.Timber;

@Module
public class TreeModule {
    @Provides
    @ApplicationScope
    Timber.Tree providesTree(CrashlyticsTreeRanger treeRanger) {
        if (BuildConfig.DEBUG || BuildConfigUtilsKt.isInternalFlavor()) {
            Video.setLogLevel(LogLevel.DEBUG);
            return new DebugTree(treeRanger);
        } else {
            return new ReleaseTree(treeRanger);
        }
    }
}
