package com.livewallpaper.audio;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class AudioLiveWallpaperActivity extends WallpaperService {

	private AudioRecord audio = null;
	private int SIZE_FFT = 1024;
	private double[] mic_data = new double[SIZE_FFT / 2];
	private double[] mic_data_prev = new double[SIZE_FFT / 2];
	private FFT fft = new FFT(SIZE_FFT);
	private int buffer_size;
	public static String CurrentTheme = "reflection";
	public static String CurrentColor = "blue";

	public static final String SHARED_PREFS_NAME = "MySettings";
	public AudioLiveWallpaperActivity thisActivity;

	/** Called when the activity is first created. */
    @Override
    public void onCreate() {
    	Log.d("AudioLiveWallPaperActivity", "onCreate");
    	thisActivity = this;
        super.onCreate();
    }

    @Override
    public void onDestroy() {
    	Log.d("AudioLiveWallPaperActivity", "onDestroy");
        super.onDestroy();
    }

	@Override
	public Engine onCreateEngine() {
		Log.d("AudioLiveWallPaperActivity", "onCreateEngine");
		return new WallpaperEngine();
	}

	class WallpaperEngine extends Engine
		implements SharedPreferences.OnSharedPreferenceChangeListener {


		private final Handler mHandler = new Handler();
		private SharedPreferences mPrefs;
		boolean mVisible = true;
		double[] window = new double[SIZE_FFT];

		private final Runnable drawWallpaper = new Runnable() {
            public void run() {

            	try {
            		drawBackground();
            	} catch (Exception e) {
            		Log.e("Debug", e.toString());
            	}
            }
        };

        WallpaperEngine() {
        	mPrefs = AudioLiveWallpaperActivity.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
        	mPrefs.registerOnSharedPreferenceChangeListener(this);
        	onSharedPreferenceChanged(mPrefs, null);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            Log.d("WallpaperEngine", "onCreate");

            // create window
            for (int i = 0; i < SIZE_FFT; i++) {
            	double a0 = 0.35875;
            	double a1 = 0.48829;
            	double a2 = 0.14128;
            	double a3 = 0.01168;
            	int N = SIZE_FFT;
            	double pi = Math.PI;
            	window[i] = a0 - a1 * Math.cos(2 * pi * i / (N - 1)) + a2 * Math.cos(4 * pi * i / (N - 1)) - a3 * Math.cos(6 * pi * i / (N - 1));
            }
        }

        @Override
        public void onDestroy() {

        	Log.d("WallpaperEngine", "onDestroy");
        	mHandler.removeCallbacks(drawWallpaper);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {

        	mVisible = visible;
            if (visible) {
            	Log.d("WallpaperEngine", "onVisibilityChanged: visible");
            	try {
            		drawBackground();
            	} catch (Exception e) {
            		Log.e("Debug", e.toString());
            	}
            } else {
                mHandler.removeCallbacks(drawWallpaper);
                Log.d("WallpaperEngine", "onVisibilityChanged: not visible");
                if (audio != null && audio.getState() == AudioRecord.STATE_INITIALIZED) {
                	audio.stop();
                	audio.release();
                	audio = null;
                	Log.d("mic data", "audio disabled");
                }
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
        	Log.d("WallpaperEngine", "onSurfaceDestroyed");
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(drawWallpaper);
        }

        float motion_event_x = 0;
        float motion_event_y = 0;

        @Override
        public void onTouchEvent(MotionEvent event) {

        	int swipe_up_gesture_dy = 250;
        	int swipe_up_gesture_dx = 75;

        	if (event.getAction() == MotionEvent.ACTION_DOWN) {
        		motion_event_x = event.getX();
        		motion_event_y = event.getY();
        	}

        	if (event.getAction() == MotionEvent.ACTION_UP) {

        		Log.d("Debug Motion Event X", Float.toString(Math.abs(event.getX() - motion_event_x)));
        		Log.d("Debug Motion Event Y", Float.toString(Math.abs(event.getY() - motion_event_y)));

        		if (Math.abs(event.getX() - motion_event_x) <= swipe_up_gesture_dx && Math.abs(event.getY() - motion_event_y) >= swipe_up_gesture_dy) {

        			String settingsActivity = WallpaperManager.getInstance(thisActivity).getWallpaperInfo().getSettingsActivity();
        			Intent intent2 = new Intent(Intent.ACTION_MAIN);
        			intent2.setComponent(new ComponentName(getPackageName(), settingsActivity)); //getPackageName());
	        		//Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
	        		intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        		// startActivity(intent2);
        		}
        	}
        }

        private void readMicData() {

        	short[] temp = null;
	        double[] temp_data = new double[SIZE_FFT];

        	try {
	        	if (audio == null) {
	        		buffer_size = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
	        		audio = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
	        		Log.d("mic data", "audio enabled");
	        	}

	        	if (audio != null && audio.getState() == AudioRecord.STATE_INITIALIZED && AudioRecord.RECORDSTATE_STOPPED == audio.getRecordingState())
	        		audio.startRecording();

	        	temp = new short[buffer_size];
	        	audio.read(temp, 0, buffer_size);

        	} catch(Exception e) {

        		Log.e("readMicData", e.toString());
        		return;
        	}

    		// window data
    		for (int j = 0; j < SIZE_FFT; j++) {
    			temp_data[j] = temp[j] * window[j];
    		}

    		// find average
    		double average = 0;
    		for (int j = 0; j < SIZE_FFT; j++) {
    			average += temp_data[j];
    		}
    		average = average / (double) SIZE_FFT;

    		// remove average
    		for (int j = 0; j < SIZE_FFT; j++) {
    			temp_data[j] -= average * 0.99;
    		}

    		// take fft
    		double[] fft_result = calc_fft(temp_data, SIZE_FFT);

    		// add to total result
    		for (int j = 0; j < mic_data.length; j++) {
    			mic_data[j] = (fft_result[j]);// + mic_data[j] * 2) / 3;
    		}

        }

        private void initMic() {

        	short[] temp = null;

        	try {
        		if (audio == null) {
	        		buffer_size = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
	        		audio = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
	        		Log.d("mic data", "audio enabled");
	        	}

	        	if (audio != null && audio.getState() == AudioRecord.STATE_INITIALIZED && AudioRecord.RECORDSTATE_STOPPED == audio.getRecordingState())
	        		audio.startRecording();

	        	temp = new short[buffer_size];
	        	audio.read(temp, 0, buffer_size);

        	} catch(Exception e) {
        		Log.e("initMic", e.toString());
        	}
        }

        private void drawBackground() throws Exception {

        	final SurfaceHolder holder = getSurfaceHolder();

        	// get access to canvas
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw wallpaper to canvas
                	if (CurrentTheme.compareTo("circle") == 0) {
                		readMicData();
                		updateWallpaperTheme3(c);
                	}
                	if (CurrentTheme.compareTo("reflection") == 0) {
                		readMicData();
                		updateWallpaperTheme1(c);
                	}
                	if (CurrentTheme.compareTo("vertical") == 0) {
                		readMicData();
                		updateWallpaperTheme2(c);
                	}
                	if (CurrentTheme.compareTo("nothing") == 0) {
                		initMic();
                		updateWallpaperTheme0(c);
                	}
                }
            } catch (Exception e) {
            	Log.e("drawBackground", e.toString());
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(drawWallpaper);
            if (mVisible) {
                mHandler.postDelayed(drawWallpaper, 20);
            }
        }

        // *****************************************************************************************
        // Themes and fft calculation functions
        //
        // *****************************************************************************************

        // use for timing
        // java.lang.System.nanoTime()

        Paint paint = new Paint();
        Paint fill = new Paint();
        Paint fill_r = new Paint();
        int red, green, blue;
        double num_input_bins = SIZE_FFT;
        double num_display_bins = num_input_bins / 16;
        double scale = 0.1;


        private void updateWallpaperTheme0(Canvas c) {

        	// do nothing
        	// c.drawColor(0xff000000);
        }

		private void updateWallpaperTheme1(Canvas c) {

        	// clear the canvas (black)
        	c.drawColor(0xff000000);

        	// calculate width of each bin
        	double width = ((double) c.getWidth() / num_display_bins);
        	int shift_up = c.getHeight() / 3;
        	int bottom = c.getHeight() - shift_up;
        	int top, left, right;
        	int shift_up_r = shift_up - 10;
        	int top_r = c.getHeight() - shift_up_r;
        	int x_shift_r, y_shift_r, bottom_r, left_r, right_r;
        	double mic_value;


        	// draw each bin to the canvas
        	for (int i = 0; i < num_display_bins; i++) {

        		if (mic_data[i] > mic_data_prev[i]) {
        			mic_value = mic_data[i];
        			mic_data_prev[i] = mic_data[i];
        		}
        		else {
        			mic_value = 0.6 * mic_data_prev[i] + 0.4 * mic_data[i];
        			mic_data_prev[i] = mic_value;
        		}

        		top = c.getHeight() - (int) (mic_value * scale) - shift_up;
        		left = (int) (width * i);
        		right = (int) (width * i + (width - 1));

        		if (mic_value == 0)
        			top = bottom;

				String colorPref = AudioLiveWallpaperActivity.CurrentColor;
				// create rectangle for ith bin
				int color2;
				int color1;
				LinearGradient linGrad;
				int color2_r;
				int color1_r;
				LinearGradient linGrad_r;
				if(colorPref.compareTo("green") == 0) {
					// Green
					color2_r = getResources().getColor(R.color.green);
					color1_r = getResources().getColor(R.color.GreenYellow);
					linGrad_r = new LinearGradient(left, bottom, left, top, new int[]{color1_r, color2_r, Color.BLACK}, null, Shader.TileMode.MIRROR);
					color2 = getResources().getColor(R.color.green);
					color1 = getResources().getColor(R.color.GreenYellow);
					linGrad = new LinearGradient(left, bottom, left, top, new int[]{color1, color2, Color.BLACK}, null, Shader.TileMode.MIRROR);
				}
				else if (colorPref.compareTo("blue") == 0) {

					//blue
					color2_r = getResources().getColor(R.color.blue);
					color1_r = getResources().getColor(R.color.cyan);
					linGrad_r = new LinearGradient(left,bottom,left,top,new int[] {color1_r,color2_r,Color.BLACK},null,Shader.TileMode.MIRROR);
					color2 = getResources().getColor(R.color.blue);
					color1 = getResources().getColor(R.color.cyan);
					linGrad = new LinearGradient(left,bottom,left,top,new int[] {color1,color2,Color.BLACK},null,Shader.TileMode.MIRROR);
				}
				else {

					//red
					color2_r = getResources().getColor(R.color.red);
					color1_r = getResources().getColor(R.color.LightPink);
					linGrad_r = new LinearGradient(left,bottom,left,top,new int[] {color1_r,color2_r,Color.BLACK},null,Shader.TileMode.MIRROR);
					color2 = getResources().getColor(R.color.red);
					color1 = getResources().getColor(R.color.LightPink);
					linGrad = new LinearGradient(left,bottom,left,top,new int[] {color1,color2,Color.BLACK},null,Shader.TileMode.MIRROR);
				}

        		fill.setShader(linGrad);
        		fill.setARGB(255, 0, 150, 190);

        		// c.drawRect(left, top, right, bottom, paint);
        		c.drawRect(left, top, right, bottom, fill);

        		// reflection
        		x_shift_r = (int) (mic_value * scale * 0.5 * Math.cos(Math.PI / 4));
        		y_shift_r = (int) (mic_value * scale * 0.5 * Math.sin(Math.PI / 4));
        		bottom_r = top_r + y_shift_r;
        		left_r = left;
        		right_r = right;


        		//blue
        		//int color2_r = getResources().getColor(R.color.blue_r);
        		//int color1_r = getResources().getColor(R.color.cyan_r);
        		//LinearGradient linGrad_r = new LinearGradient(left,bottom,left,top,new int[] {color1_r,color2_r,Color.BLACK},null,Shader.TileMode.MIRROR);




        		fill_r.setShader(linGrad_r);
        		fill_r.setARGB(150, 10, 150, 190);

        		Path path = new Path();
        		path.moveTo(left_r, top_r);
        		path.lineTo(right_r, top_r);
        		path.lineTo(right_r - x_shift_r, bottom_r);
        		path.lineTo(left_r - x_shift_r, bottom_r);
        		path.lineTo(left_r, top_r);
        		c.drawPath(path, fill_r);

        	}
        }

        private void updateWallpaperTheme2(Canvas c) {
        	// set up the color of the rectangles
        	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        	Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

        	int red;
        	int green;
        	int blue;

        	// clear the canvas (black)
        	c.drawColor(0xff000000);
        	//c.drawColor(0xff0099CC);
        	//c.drawBitmap(background, null, new Rect(0, 0, c.getWidth(), c.getHeight()), null);

        	//double num_input_bins = SIZE_FFT; // needs to be 2^x
        	//double num_display_bins = num_input_bins / 8;
        	//double scale = 30.0;
        	double scale = 0.005;

        	// create random temp values because mic doesn't work
        	// in emulator
        	for (int i = 0; i < num_input_bins; i++) {
        		// mic_data[i] = (short) ((random_number.nextInt(500) + mic_data[i] * 2) / 3);
        	}

        	// check for valid data in mic_data


        	// take fft of mic_data
        	// int[] fft_result = calc_fft(mic_data, num_input_bins);

        	// calculate width of each bin
        	double width = ((double) c.getWidth() / num_display_bins);

        	// draw each bin to the canvas
        	//LinearGradient linGrad = new LinearGradient(0,0,c.getWidth(),0,Color.RED,Color.BLUE,Shader.TileMode.MIRROR);

        	for (int i = 0; i < num_display_bins; i++) {
        		//int top = c.getHeight() - (int) (Math.log(mic_data[i]) * scale);
        		//int top = c.getHeight() - c.getHeight()/3 - (int) (Math.log(mic_data[i]) * scale);
        		int top = c.getHeight() - c.getHeight()/3 - (int) ((mic_data[i]) * scale);
        		int bottom = c.getHeight() - c.getHeight()/3;
        		//int ref_bottom = c.getHeight() - c.getHeight()/3 + (int) (Math.log(mic_data[i]) * scale);
        		int ref_bottom = c.getHeight() - c.getHeight()/3 + (int) ((mic_data[i]) * scale);
        		int left = (int) (width * i);
        		int right = (int) (width * i + (width - 1));
        		//LinearGradient linGrad = new LinearGradient(left,bottom,left,top,new int[] {Color.CYAN,Color.BLUE,Color.BLACK},null,Shader.TileMode.MIRROR);
        		//LinearGradient linGrad = new LinearGradient(left,bottom,left,top,new int[] {0xFF99CC00,0xFFFF0000,Color.BLACK},null,Shader.TileMode.MIRROR);

				String colorPref = AudioLiveWallpaperActivity.CurrentColor;
				int color2;
				int color1;
				LinearGradient linGrad;
				if(colorPref.compareTo("green") == 0) {
					// Green
					color2 = getResources().getColor(R.color.green);
					color1 = getResources().getColor(R.color.GreenYellow);
					linGrad = new LinearGradient(left, bottom, left, top, new int[]{color1, color2, Color.BLACK}, null, Shader.TileMode.MIRROR);
				}
				else if (colorPref.compareTo("blue") == 0) {

					//blue
					color2 = getResources().getColor(R.color.blue);
					color1 = getResources().getColor(R.color.cyan);
					linGrad = new LinearGradient(left,bottom,left,top,new int[] {color1,color2,Color.BLACK},null,Shader.TileMode.MIRROR);
				}
				else {

					//red
					color2 = getResources().getColor(R.color.red);
					color1 = getResources().getColor(R.color.LightPink);
					linGrad = new LinearGradient(left,bottom,left,top,new int[] {color1,color2,Color.BLACK},null,Shader.TileMode.MIRROR);
				}

        		red = 255;
            	green = 255;
            	blue = 255;

            	paint.setARGB(255, red, green, blue);
        		paint.setStrokeWidth(1);
        		paint.setStyle(Paint.Style.STROKE);

        		//fill.setARGB(255, 255, 255, 255);
        		fill.setShader(linGrad);

        		// c.drawRect(left, top, right, bottom, paint);
        		c.drawRect(left, top, right, bottom, fill);
        		c.drawRect(left,bottom,right,ref_bottom,fill);

        	}
        }

        private void updateWallpaperTheme3(Canvas c) {

        	// clear the canvas (black)
        	c.drawColor(0xff000000);

        	double num_input_bins = SIZE_FFT; // needs to be 2^x
        	double num_display_bins = num_input_bins / 8;
        	float scale = 10;

        	float center_x = c.getWidth() / 2;
        	float center_y = c.getHeight() / 2;

        	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        	paint.setStrokeWidth(scale);
        	double color_r = 0;
        	double color_g = 0;
        	double color_b = 0;
        	String color_s = String.format("#%02x%02x%02x", (int) color_r, (int) color_g, (int) color_b);
        	int color = Color.parseColor(color_s);

        	// draw each bin to the canvas
        	for (int i = (int) (num_display_bins - 1); i >= 0; i--) {

        		int idx = (int) num_display_bins - 1 - i;

        		color_r = (mic_data[idx] / 90000);
        		color_r = color_r > 1.0 ? 255 : color_r * 255;
        		color_g = (mic_data[idx] / 30000);
        		color_g = color_g > 1.0 ? 255 : color_g * 255;
        		color_b = (mic_data[idx] / 60000);
        		color_b = color_b > 1.0 ? 255 : color_b * 255;

        		color_s = String.format("#%02x%02x%02x", (int) color_r, (int) color_g, (int) color_b);
        		color = Color.parseColor(color_s);

        		paint.setColor(color);
        		c.drawCircle(center_x, center_y, i * scale, paint);
        	}
        }

        private double[] calc_fft(double[] data, int size_data) {
        	double[] real = new double[size_data];
        	double[] imag = new double[size_data];
        	double[] result = new double[size_data / 2];

        	for (int i = 0; i < size_data; i++) {
        		real[i] = data[i];
        		imag[i] = 0;
        	}

        	fft.fft(real, imag);

        	// calculate the magnitude and store in result
        	for (int i = 0; i < size_data / 2; i++) {
        		result[i] = Math.sqrt(Math.pow(real[i], 2) + Math.pow(imag[i], 2));
        	}

        	return result;
        }
	}
}