package com.mapbox.mapboxsdk.testapp.activity.render;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.google.gson.Gson;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter;
import okio.BufferedSource;
import okio.Okio;
import timber.log.Timber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity that generates map snapshots based on the node render test suite.
 */
public class RenderTestActivity extends AppCompatActivity {

  private static final String RENDER_TEST_BASE_PATH = "integration/render-tests";

  // TODO read out excluded tests from /platform/node/test/ignore.json
  private static final List<String> EXCLUDED_TESTS = new ArrayList<String>() {{
    add("overlay,background-opacity");
    add("collision-lines-pitched,debug");
    add("1024-circle,extent");
    add("empty,empty");
    add("rotation-alignment-map,icon-pitch-scaling");
    add("rotation-alignment-viewport,icon-pitch-scaling");
    add("pitch15,line-pitch");
    add("pitch30,line-pitch");
    add("line-placement-true-pitched,text-keep-upright");
    add("180,raster-rotation");
    add("45,raster-rotation");
    add("90,raster-rotation");
    add("mapbox-gl-js#5631,regressions"); // crashes
    add("overlapping,raster-masking");
    add("missing,raster-loading");
    add("pitchAndBearing,line-pitch");
  }};

  private final Map<RenderTestDefinition, Bitmap> renderResultMap = new HashMap<>();
  private List<RenderTestDefinition> renderTestDefinitions;
  private OnSnapshotReadyListener onSnapshotReadyListener;
  private MapSnapshotter mapSnapshotter;
  private ImageView imageView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(imageView = new ImageView(RenderTestActivity.this));
    imageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
    new LoadRenderDefinitionTask(this).execute();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mapSnapshotter != null) {
      mapSnapshotter.cancel();
    }
  }

  //
  // Loads the render test definitions from assets folder
  //
  private static class LoadRenderDefinitionTask extends AsyncTask<Void, Void, List<RenderTestDefinition>> {

    private WeakReference<RenderTestActivity> renderTestActivityWeakReference;

    LoadRenderDefinitionTask(RenderTestActivity renderTestActivity) {
      this.renderTestActivityWeakReference = new WeakReference<>(renderTestActivity);
    }

    @Override
    protected List<RenderTestDefinition> doInBackground(Void... voids) {
      List<RenderTestDefinition> definitions = new ArrayList<>();
      AssetManager assetManager = renderTestActivityWeakReference.get().getAssets();
      String[] categories = new String[0];
      try {
        categories = assetManager.list(RENDER_TEST_BASE_PATH);
      } catch (IOException exception) {
        Timber.e(exception);
      }
      for (int counter = categories.length - 1; counter >= 0; counter--) {
        try {
          String[] tests = assetManager.list(String.format("%s/%s", RENDER_TEST_BASE_PATH, categories[counter]));
          for (String test : tests) {
            String styleJson = null;
            try {
              styleJson = loadStyleJson(assetManager, categories[counter], test);
            } catch (IOException exception) {
              Timber.e(exception);
            }

            RenderTestStyleDefinition renderTestStyleDefinition = new Gson().fromJson(styleJson, RenderTestStyleDefinition.class);
            RenderTestDefinition definition = new RenderTestDefinition(categories[counter], test, styleJson, renderTestStyleDefinition);
            if (!definition.hasOperations()) {
              if (!definition.getCategory().equals("combinations") && !EXCLUDED_TESTS.contains(definition.getName() + "," + definition.getCategory())) {
                definitions.add(definition);
              }
            } else {
              Timber.e("could not add test, test requires operations: %s from %s", test, categories[counter]);
            }
          }
        } catch (Exception exception) {
          Timber.e(exception);
        }
      }
      return definitions;
    }

    @Override
    protected void onPostExecute(List<RenderTestDefinition> renderTestDefinitions) {
      super.onPostExecute(renderTestDefinitions);
      RenderTestActivity renderTestActivity = renderTestActivityWeakReference.get();
      if (renderTestActivity != null) {
        renderTestActivity.startRenderTests(renderTestDefinitions);
      }
    }

    private static String loadStyleJson(AssetManager assets, String category, String test) throws IOException {
      InputStream input = assets.open(String.format("%s/%s/%s/style.json", RENDER_TEST_BASE_PATH, category, test));
      BufferedSource source = Okio.buffer(Okio.source(input));
      String output = source.readByteString().string(Charset.forName("utf-8"));
      input.close();
      return output;
    }
  }

  private void startRenderTests(List<RenderTestDefinition> renderTestDefinitions) {
    this.renderTestDefinitions = renderTestDefinitions;
    if (!renderTestDefinitions.isEmpty()) {
      render(renderTestDefinitions.get(0), renderTestDefinitions.size());
    }
  }

  private void render(final RenderTestDefinition renderTestDefinition, final int testSize) {
    Timber.d("Render test %s,%s", renderTestDefinition.getName(), renderTestDefinition.getCategory());
    mapSnapshotter = new RenderTestSnapshotter(this, renderTestDefinition.toOptions());
    mapSnapshotter.start(result -> {
      Bitmap snapshot = result.getBitmap();
      imageView.setImageBitmap(snapshot);
      renderResultMap.put(renderTestDefinition, snapshot);
      if (renderResultMap.size() != testSize) {
        continueTesting(renderTestDefinition);
      } else {
        finishTesting();
      }
    }, error -> Timber.e(error));
  }

  private void continueTesting(RenderTestDefinition renderTestDefinition) {
    int next = renderTestDefinitions.indexOf(renderTestDefinition) + 1;
    Timber.d("Next test: %s / %s", next, renderTestDefinitions.size());
    render(renderTestDefinitions.get(next), renderTestDefinitions.size());
  }

  private void finishTesting() {
    new SaveResultToDiskTask(onSnapshotReadyListener, renderResultMap).execute();
  }

  //
  // Save tests results to disk
  //
  private static class SaveResultToDiskTask extends AsyncTask<Void, Void, Void> {

    private OnSnapshotReadyListener onSnapshotReadyListener;
    private Map<RenderTestDefinition, Bitmap> renderResultMap;

    SaveResultToDiskTask(OnSnapshotReadyListener onSnapshotReadyListener, Map<RenderTestDefinition, Bitmap> renderResultMap) {
      this.onSnapshotReadyListener = onSnapshotReadyListener;
      this.renderResultMap = renderResultMap;
    }

    @Override
    protected Void doInBackground(Void... voids) {
      if (isExternalStorageWritable()) {
        try {
          File testResultDir = FileUtils.createTestResultRootFolder();
          String basePath = testResultDir.getAbsolutePath();
          for (Map.Entry<RenderTestDefinition, Bitmap> testResult : renderResultMap.entrySet()) {
            writeResultToDisk(basePath, testResult);
          }
        } catch (final Exception exception) {
          Timber.e(exception);
        }
      }
      return null;
    }

    private void writeResultToDisk(String basePath, Map.Entry<RenderTestDefinition, Bitmap> testResult) throws IOException {
      RenderTestDefinition definition = testResult.getKey();
      String categoryName = definition.getCategory();
      String categoryPath = String.format("%s/%s", basePath, categoryName);
      FileUtils.createCategoryDirectory(categoryPath);
      String testName = testResult.getKey().getName();
      String testDir = FileUtils.createTestDirectory(categoryPath, testName);
      FileUtils.writeTestResultToDisk(testDir, testResult.getValue());
    }

    private boolean isExternalStorageWritable() {
      return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    @Override
    protected void onPostExecute(Void aVoid) {
      super.onPostExecute(aVoid);
      if (onSnapshotReadyListener != null) {
        onSnapshotReadyListener.onSnapshotReady();
      }
    }
  }

  //
  // Callback configuration to notify test executor of test finishing
  //
  public interface OnSnapshotReadyListener {
    void onSnapshotReady();
  }

  public void setOnSnapshotReadyListener(OnSnapshotReadyListener listener) {
    this.onSnapshotReadyListener = listener;
  }

  //
  // FileUtils
  //

  private static class FileUtils {

    private static void createCategoryDirectory(String catPath) {
      File testResultDir = new File(catPath);
      if (testResultDir.exists()) {
        return;
      }

      if (!testResultDir.mkdirs()) {
        throw new RuntimeException("can't create root test directory");
      }
    }

    private static File createTestResultRootFolder() {
      File testResultDir = new File(Environment.getExternalStorageDirectory() + File.separator + "mapbox");
      if (testResultDir.exists()) {
        // cleanup old files
        deleteRecursive(testResultDir);
      }

      if (!testResultDir.mkdirs()) {
        throw new RuntimeException("can't create root test directory");
      }
      return testResultDir;
    }

    private static void deleteRecursive(File fileOrDirectory) {
      if (fileOrDirectory.isDirectory()) {
        File[] files = fileOrDirectory.listFiles();
        if (files != null) {
          for (File file : files) {
            deleteRecursive(file);
          }
        }
      }

      if (!fileOrDirectory.delete()) {
        Timber.e("can't delete directory");
      }
    }

    private static String createTestDirectory(String basePath, String testName) {
      File testDir = new File(basePath + "/" + testName);
      if (!testDir.exists()) {
        if (!testDir.mkdir()) {
          throw new RuntimeException("can't create sub directory for " + testName);
        }
      }
      return testDir.getAbsolutePath();
    }

    private static void writeTestResultToDisk(String testPath, Bitmap testResult) throws IOException {
      String filePath = testPath + "/actual.png";
      FileOutputStream out = new FileOutputStream(filePath);
      testResult.compress(Bitmap.CompressFormat.PNG, 100, out);
      out.flush();
      out.close();
    }
  }
}
