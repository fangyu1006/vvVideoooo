package com.example.fangy.vshare;

/**
 * Created by fangy on 2017/10/30.
 */

import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class VideoClip {
    private static final String TAG = "VideoClip";
    private static String filePath;
    private static String workingPath;
    private static String outName;
    private static double startTime;
    private static double endTime;

    public static void setFilePath(String filePathIn) {
        filePath = filePathIn;
    }

    public static void setWorkingPath(String workingPathIn) {
        workingPath = workingPathIn;
    }

    public static  void setOutName(String outNameIn) {
        outName = outNameIn;
    }

    public static void setEndTime(double endTimeIn) {
        endTime = endTimeIn / 1000;
        Log.e(TAG, "End Time: " + endTime);

    }

    public static void setStartTime(double startTimeIn) {
        startTime = startTimeIn / 1000;
        Log.e(TAG, "Start Time: " + startTime);
    }

    public static void clip() {
        try {
            //Video file to be clipped 
            Movie movie = MovieCreator.build(filePath);

            List<Track> tracks = movie.getTracks();
            movie.setTracks(new LinkedList<Track>());

            boolean timeCorrected = false;

            //Calculate th time of clipping 
            for (Track track : tracks) {
                if (track.getSyncSamples() != null
                        && track.getSyncSamples().length > 0) {
                    if (timeCorrected) {
                        throw new RuntimeException(
                                "The startTime has already been corrected by another track with SyncSample. Not Supported.");
                    }//true,false for short clip；false,true for long clip  
                    startTime = correctTimeToSyncSample(track, startTime, false);
                    endTime = correctTimeToSyncSample(track, endTime, true);
                    timeCorrected = true;
                }
            }
            //Clip video based on time  
            for (Track track : tracks) {
                long currentSample = 0; //current time  
                double currentTime = 0; //time of current
                double lastTime = -1; //last time 
                long startSample1 = -1; //beginning time
                long endSample1 = -1; //end time 

                // Set start time and end time to avoid over clipping 
                for (int i = 0; i < track.getSampleDurations().length; i++) {
                    long delta = track.getSampleDurations()[i];
                    if (currentTime > lastTime && currentTime <= startTime) {
                        startSample1 = currentSample;//edit start time 
                    }
                    if (currentTime > lastTime && currentTime <= endTime) {
                        endSample1 = currentSample; //edit end time 
                    }

                    lastTime = currentTime;
                    currentTime += (double) delta
                            / (double) track.getTrackMetaData().getTimescale();
                    currentSample++;
                }
                movie.addTrack(new CroppedTrack(track, startSample1, endSample1));// Create a new video file
            }

            //merge to  mp4  
            Container out = new DefaultMp4Builder().build(movie);
            File storagePath = new File(workingPath);
            storagePath.mkdirs();
            FileOutputStream fos = new FileOutputStream(new File(storagePath, outName));
            FileChannel fco = fos.getChannel();
            out.writeContainer(fco);

            fco.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;

        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];
            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(),
                        currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta
                    / (double) track.getTrackMetaData().getTimescale();
            currentSample++;
        }

        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}