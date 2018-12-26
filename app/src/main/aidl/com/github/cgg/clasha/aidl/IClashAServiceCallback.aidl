package com.github.cgg.clasha.aidl;

import com.github.cgg.clasha.aidl.TrafficStats;

oneway interface IClashAServiceCallback {
  void stateChanged(int state, String profileName, String msg);
  void trafficUpdated(long profileId, in TrafficStats stats);
  // Traffic data has persisted to database, listener should refetch their data from database
  void trafficPersisted(long profileId);
}
