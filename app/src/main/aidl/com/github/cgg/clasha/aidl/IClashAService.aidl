package com.github.cgg.clasha.aidl;

import com.github.cgg.clasha.aidl.IClashAServiceCallback;

interface IClashAService {
  int getState();
  String getProfileName();

  void registerCallback(in IClashAServiceCallback cb);
  void startListeningForBandwidth(in IClashAServiceCallback cb, long timeout);
  oneway void stopListeningForBandwidth(in IClashAServiceCallback cb);
  oneway void unregisterCallback(in IClashAServiceCallback cb);
}
