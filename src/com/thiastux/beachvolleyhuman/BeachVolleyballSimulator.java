package com.thiastux.beachvolleyhuman;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.*;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.thiastux.beachvolleyhuman.model.Ball;
import com.thiastux.beachvolleyhuman.model.Stickman;

import java.util.HashMap;

public class BeachVolleyballSimulator extends SimpleApplication {

    private Stickman stickman;
    private HashMap<Integer, Spatial> skeletonMap = new HashMap<>();
    private Ball ball;
    private Geometry terrainGeometry;
    private float timeElapsed = 0;
    private BulletAppState bulletAppState;
    private RigidBodyControl ballPhy;

    private BeachVolleyballSimulator() {
        super();
    }

    public static void main(String[] args) {

        BeachVolleyballSimulator app = new BeachVolleyballSimulator();
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1920);
        settings.setHeight(1052);
        settings.setSamples(16);
        settings.setVSync(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        initPhysics();
        setCamera();
        setDebugInfo();
        addReferenceSystem();
        createHuman();
        createBall();
        createTerrain();
        createCourt();
        createTargets();
        setLight();

    }


    @Override
    public void simpleUpdate(float tpf) {
        timeElapsed += tpf;
        if (timeElapsed >= 10) {
            System.out.println("Direction" + cam.getDirection().toString());
            System.out.println("Location:" + cam.getLocation());
            System.out.println("Rotation:" + cam.getRotation().toString());
            timeElapsed = 0;
        }

    }

    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
    }

    private void setCamera() {
        flyCam.setEnabled(false);
        //flyCam.setMoveSpeed(50);
        cam.setLocation(new Vector3f(0.62f, 10f, -18));
        cam.lookAtDirection(new Vector3f(0f, -0.25f, 0.9f), Vector3f.UNIT_Y);
    }

    private void setDebugInfo() {
        setDisplayFps(true);
        setDisplayStatView(true);
        setPauseOnLostFocus(false);
    }

    private void addReferenceSystem() {

        Node refNode = new Node("RefNode");

        Line xAxisline = new Line(new Vector3f(0, 0, 0), new Vector3f(3, 0, 0));
        Geometry xAxisGeometry = new Geometry("xAxis", xAxisline);
        Material xLineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        xLineMaterial.getAdditionalRenderState().setLineWidth(2);
        xLineMaterial.setColor("Color", ColorRGBA.Green);
        xAxisGeometry.setMaterial(xLineMaterial);

        Line yAxisline = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 3, 0));
        Geometry yAxisGeometry = new Geometry("yAxis", yAxisline);
        Material yLineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        yLineMaterial.getAdditionalRenderState().setLineWidth(2);
        yLineMaterial.setColor("Color", ColorRGBA.Blue);
        yAxisGeometry.setMaterial(yLineMaterial);

        Line zAxisline = new Line(new Vector3f(0, 0, 0), new Vector3f(0, 0, 3));
        Geometry zAxisGeometry = new Geometry("zAxis", zAxisline);
        Material zLineMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        zLineMaterial.getAdditionalRenderState().setLineWidth(2);
        zLineMaterial.setColor("Color", ColorRGBA.Red);
        zAxisGeometry.setMaterial(zLineMaterial);

        refNode.attachChild(xAxisGeometry);
        refNode.attachChild(yAxisGeometry);
        refNode.attachChild(zAxisGeometry);

        refNode.setLocalTranslation(0, 0, 0);

        rootNode.attachChild(refNode);
    }

    private void createHuman() {
        stickman = new Stickman(rootNode, skeletonMap, assetManager);
    }

    private void createBall() {
        Sphere ballMesh = new Sphere(20, 20, 1f);
        Geometry ballGeometry = new Geometry("Sphere", ballMesh);
        Material ballMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        ballMat.setBoolean("UseMaterialColors", true);
        ballMat.setColor("Ambient", ColorRGBA.Orange);
        ballMat.setColor("Diffuse", ColorRGBA.Orange);
        ballGeometry.setMaterial(ballMat);

        rootNode.attachChild(ballGeometry);
        ballGeometry.setLocalTranslation(-5, 0, 0);

        ballPhy = new RigidBodyControl(1f);
        ballGeometry.addControl(ballPhy);
    }

    private void createTerrain() {
        float TERRAIN_WIDTH = 250f;
        float TERRAIN_HEIGHT = 750f;
        Quad terrainMesh = new Quad(TERRAIN_WIDTH, TERRAIN_HEIGHT);
        terrainGeometry = new Geometry("Terrain", terrainMesh);
        terrainGeometry.setLocalRotation(new Quaternion().fromAngles((float) Math.toRadians(-90), 0f, 0f));
        terrainGeometry.setLocalTranslation(-TERRAIN_WIDTH / 2, -(stickman.TORSO_HEIGHT / 2 + stickman.ULEG_LENGTH + stickman.LLEG_LENGTH), TERRAIN_HEIGHT * .75f);
        Material terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        /*TextureKey sandTextureKey = new TextureKey("assets/sand.jpg");
        sandTextureKey.setGenerateMips(true);
        Texture sandTexture = assetManager.loadTexture(sandTextureKey);
        sandTexture.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("ColorMap", sandTexture);*/
        terrainMaterial.setBoolean("UseMaterialColors", true);
        ColorRGBA sandColor = new ColorRGBA(.835f, .792f, .659f, 1f);
        terrainMaterial.setColor("Ambient", sandColor);
        terrainMaterial.setColor("Diffuse", sandColor);
        terrainGeometry.setMaterial(terrainMaterial);
        terrainGeometry.setShadowMode(ShadowMode.Receive);

        rootNode.attachChild(terrainGeometry);
    }

    private void createCourt() {
        Node courtNode = new Node();

        float courtLength = 50f;
        float courtWidth = 25f;
        float lineWidth = 0.2f;
        float lineThickness = 0.01f;
        float netHeight = 16f;
        float poleThickness = 0.3f;

        Box sxLine = new Box(lineWidth, courtLength, lineThickness);
        Geometry sxLineGeom = new Geometry("sxLine", sxLine);
        sxLineGeom.setLocalTranslation(courtWidth, courtLength, 0);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        sxLineGeom.setMaterial(mat);

        Box dxLine = new Box(lineWidth, courtLength, lineThickness);
        Geometry dxLineGeom = new Geometry("dxLine", dxLine);
        dxLineGeom.setLocalTranslation(-courtWidth, courtLength, 0);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        dxLineGeom.setMaterial(mat);

        Box btLine = new Box(courtWidth, lineWidth, lineThickness);
        Geometry btLineGeom = new Geometry("btLine", btLine);
        btLineGeom.setLocalTranslation(0f, 0, 0);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        btLineGeom.setMaterial(mat);

        Box tpLine = new Box(courtWidth, lineWidth, lineThickness);
        Geometry tpLineGeom = new Geometry("tpLine", tpLine);
        tpLineGeom.setLocalTranslation(0f, courtLength * 2, 0);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);
        tpLineGeom.setMaterial(mat);

        Cylinder leftPole = new Cylinder(50, 50, poleThickness, netHeight, true);
        Geometry leftPoleGeometry = new Geometry("leftPole", leftPole);
        leftPoleGeometry.setLocalTranslation(-courtWidth, courtLength, -netHeight / 2);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Yellow);
        mat.setColor("Diffuse", ColorRGBA.Yellow);
        leftPoleGeometry.setMaterial(mat);

        Cylinder rightPole = new Cylinder(50, 50, poleThickness, netHeight, true);
        Geometry rightPoleGeometry = new Geometry("rightPole", rightPole);
        rightPoleGeometry.setLocalTranslation(courtWidth, courtLength, -netHeight / 2);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Yellow);
        mat.setColor("Diffuse", ColorRGBA.Yellow);
        rightPoleGeometry.setMaterial(mat);

        Box lNetLine = new Box(courtWidth, lineThickness, lineWidth);
        Geometry lNetLineGeom = new Geometry("lNetLine", lNetLine);
        lNetLineGeom.setLocalTranslation(0f, courtLength, -netHeight / 2 - 1);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        lNetLineGeom.setMaterial(mat);

        Box uNetLine = new Box(courtWidth, lineThickness, lineWidth);
        Geometry uNetLineGeom = new Geometry("uNetLine", uNetLine);
        uNetLineGeom.setLocalTranslation(0f, courtLength, -netHeight);
        mat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Red);
        mat.setColor("Diffuse", ColorRGBA.Red);
        uNetLineGeom.setMaterial(mat);


        courtNode.attachChild(sxLineGeom);
        courtNode.attachChild(dxLineGeom);
        courtNode.attachChild(btLineGeom);
        courtNode.attachChild(tpLineGeom);
        courtNode.attachChild(leftPoleGeometry);
        courtNode.attachChild(rightPoleGeometry);
        courtNode.attachChild(lNetLineGeom);
        courtNode.attachChild(uNetLineGeom);
        for (Spatial child : courtNode.getChildren()) {
            child.setShadowMode(ShadowMode.Receive);
        }


        float[] rotAngles = new float[]{(float) Math.toRadians(90), 0, 0};
        Quaternion rotQuat = new Quaternion(rotAngles);
        courtNode.setLocalRotation(rotQuat);
        courtNode.setLocalTranslation(0, -(stickman.TORSO_HEIGHT / 2 + stickman.ULEG_LENGTH + stickman.LLEG_LENGTH) + 0.01f, 1f);

        rootNode.attachChild(courtNode);
    }


    private void createTargets() {

    }

    private void setLight() {
        float light_intensity = .30f;
        //Add light to the scene
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White.mult(light_intensity));
        sun.setDirection(new Vector3f(.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun);

        DirectionalLight sun2 = new DirectionalLight();
        sun2.setColor(ColorRGBA.White.mult(light_intensity));
        sun2.setDirection(new Vector3f(-.5f, -.5f, -.5f).normalizeLocal());
        rootNode.addLight(sun2);

        DirectionalLight sun3 = new DirectionalLight();
        sun3.setColor(ColorRGBA.White.mult(light_intensity));
        sun3.setDirection(new Vector3f(-.5f, -.5f, .5f).normalizeLocal());
        rootNode.addLight(sun3);

        DirectionalLight sun4 = new DirectionalLight();
        sun4.setColor(ColorRGBA.White.mult(light_intensity));
        sun4.setDirection(new Vector3f(.5f, -.5f, .5f).normalizeLocal());
        rootNode.addLight(sun4);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(.1f));
        rootNode.addLight(al);

        rootNode.setShadowMode(ShadowMode.Off);

        stickman.setShadowMode(ShadowMode.Cast);

        final int SHADOWMAP_SIZE = 1024;
        DirectionalLightShadowRenderer dlsr = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr.setLight(sun);
        DirectionalLightShadowRenderer dlsr2 = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr2.setLight(sun2);
        DirectionalLightShadowRenderer dlsr3 = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr3.setLight(sun3);
        DirectionalLightShadowRenderer dlsr4 = new DirectionalLightShadowRenderer(assetManager, SHADOWMAP_SIZE, 3);
        dlsr4.setLight(sun4);
        viewPort.addProcessor(dlsr);
        viewPort.addProcessor(dlsr2);
        viewPort.addProcessor(dlsr3);
        viewPort.addProcessor(dlsr4);

        viewPort.setBackgroundColor(ColorRGBA.Cyan);

    }
}
