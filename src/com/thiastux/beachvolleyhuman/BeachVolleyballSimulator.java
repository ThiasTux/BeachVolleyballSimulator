package com.thiastux.beachvolleyhuman;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.*;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.thiastux.beachvolleyhuman.model.Const;
import com.thiastux.beachvolleyhuman.model.Stickman;
import com.thiastux.beachvolleyhuman.model.TrajectoriesCreator;
import com.thiastux.beachvolleyhuman.model.Trajectory;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import javax.annotation.Nonnull;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class BeachVolleyballSimulator extends SimpleApplication implements PhysicsCollisionListener, PhysicsTickListener, ScreenController {

    private static int MAX_SHOTS = 3;
    private static boolean DEBUG = false;
    private TCPDataClient tcpDataClient;
    private Stickman stickman;
    private HashMap<Integer, Spatial> skeletonMap = new HashMap<>();
    private Geometry ballGeometry;
    private BulletAppState bulletAppState;
    private RigidBodyControl ballPhy;
    private Quaternion[] animationQuaternions;
    private Quaternion preRot;
    private Quaternion[] previousQuaternions = new Quaternion[12];
    private int numEvent;
    private RigidBodyControl rHandControl;
    private TextRenderer numShotsTextview;
    private TextRenderer scoreTextview;
    private TextRenderer playerChartsTextview;
    private int numShots = 0;
    private int score = 0;
    private float elapsedTime = 0;
    private Vector3f prevHandPosition;
    private List<Float> prevDist = new ArrayList<>();
    private List<Vector3f> prevPos = new ArrayList<>();
    private float courtWidth = 25f;
    private float courtLength = courtWidth * 2;
    private float lineWidth = 0.2f;
    private float lineThickness = 0.01f;
    private float netHeight = 16f;
    private float poleThickness = 0.3f;
    private List<Trajectory> trajectories;
    private Trajectory currTrajectory;
    private ActionListener actionListener = (name, isPressed, tpf) -> {
        switch (name) {
            case "TossBall":
                if (!isPressed)
                    createBall();
                break;
            case "ResetCamera":
                if (!isPressed) {
                    System.out.println("ResetCamera");
                    setCamera();
                }
                break;
            case "ResetGame":
                if (!isPressed) {
                    System.out.println("ResetGame");
                    resetGame();
                }
                break;
        }
    };
    private BufferedWriter writer;


    private BeachVolleyballSimulator(String[] args) {
        tcpDataClient = new TCPDataClient(this, args);
    }

    public static void main(String[] args) {

        DEBUG = Boolean.parseBoolean(args[0]);

        BeachVolleyballSimulator app = new BeachVolleyballSimulator(Arrays.copyOfRange(args, 3, args.length));
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(Integer.parseInt(args[1]));
        settings.setHeight(Integer.parseInt(args[2]));
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
        initInterface();
        addReferenceSystem();
        createStickman();
        createTerrain();
        createCourt();
        createTargets();
        setLight();
        initKeys();
        computeInitialQuaternions();
        initTrajectories();
        //createBall();


        tcpDataClient.startExecution();

    }

    @Override
    public void simpleUpdate(float tpf) {
        elapsedTime += tpf;
        boolean animStart = Const.animationStart;
        if (animStart) {
            getData();
            animateModel();
        } else {
            //stickman.animateBone(2, 0, true);
        }
    }

    @Override
    public void stop() {
        if (!DEBUG) {
            tcpDataClient.stopExecution();
        }
        System.out.println("\nApplication ended");
        super.stop();
    }

    private void getData() {
        animationQuaternions = tcpDataClient.getData();
    }

    private void animateModel() {
        for (int i = 0; i < 12; i++) {
            Quaternion rotQuat = preProcessingQuaternion(i);
            if (rotQuat != null && i != 0) {
                stickman.updateModelBonePosition(rotQuat, i);
            }
        }
    }

    private Quaternion preProcessingQuaternion(int i) {
        if (animationQuaternions[i] == null) {
            return null;
        }

        //Normalize quaternion to adjust lost of precision using mG.
        Quaternion outputQuat = animationQuaternions[i].normalizeLocal();

        outputQuat = outputQuat.mult(preRot);
        outputQuat = new Quaternion(outputQuat.getX(), -outputQuat.getY(), outputQuat.getZ(), outputQuat.getW());
        previousQuaternions[i] = outputQuat.normalizeLocal();
        outputQuat = conjugate(getPrevLimbQuaternion(i)).mult(outputQuat);
        outputQuat = outputQuat.normalizeLocal();

        return outputQuat;
    }

    private Quaternion conjugate(Quaternion quaternion) {
        return new Quaternion(-quaternion.getX(), -quaternion.getY(), -quaternion.getZ(), quaternion.getW());
    }

    private Quaternion getPrevLimbQuaternion(int i) {
        switch (i) {
            case 1:
            case 3:
            case 4:
            case 6:
            case 7:
            case 9:
            case 11:
                return previousQuaternions[i - 1];
            case 2:
            case 5:
            case 8:
            case 10:
                return previousQuaternions[0];
            default:
                return Quaternion.IDENTITY;
        }

    }

    private void computeInitialQuaternions() {
        // Compose two rotations:
        // First, rotate the rendered model to face inside the screen (negative z)
        // Then, rotate the rendered model to have the torso horizontal (facing downwards, leg facing north)
        Quaternion quat1 = new Quaternion().fromAngles(0f, 0f, (float) Math.toRadians(90));
        Quaternion quat2 = new Quaternion().fromAngles((float) Math.toRadians(-90), 0f, 0f);
        preRot = quat1.mult(quat2);

        String print = String.format("qPreRot: %.1f %.1f %.1f %.1f", preRot.getW(), preRot.getX(), preRot.getY(), preRot.getZ());
        System.out.println(print + "    ");

        Quaternion qAlignArmR = new Quaternion().fromAngles(0f, 0f, (float) Math.toRadians(90));
        print = String.format("qRArmRot: %.1f %.1f %.1f %.1f", qAlignArmR.getW(), qAlignArmR.getX(), qAlignArmR.getY(), qAlignArmR.getZ());
        System.out.println(print + "    ");

        Quaternion qAlignArmL = new Quaternion().fromAngles(0f, 0f, (float) Math.toRadians(-90));
        print = String.format("qLArmRot: %.1f %.1f %.1f %.1f", qAlignArmL.getW(), qAlignArmL.getX(), qAlignArmL.getY(), qAlignArmL.getZ());
        System.out.println(print + "    ");

        for (int i = 0; i < 12; i++) {
            previousQuaternions[i] = new Quaternion();
        }

    }

    private void initPhysics() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //bulletAppState.setDebugEnabled(true);
        bulletAppState.getPhysicsSpace().setGravity(new Vector3f(0, -19.81f, 0));
        //bulletAppState.getPhysicsSpace().setAccuracy(1f / 120f);
        bulletAppState.getPhysicsSpace().addTickListener(this);
    }

    private void setCamera() {
        if (!DEBUG) {
            flyCam.setEnabled(false);
            cam.setLocation(new Vector3f(0.62f, 20f, -35));
            cam.lookAtDirection(new Vector3f(0f, -0.30f, 0.9f), Vector3f.UNIT_Y);
            /*cam.setLocation(new Vector3f(0f, 50f, -35));
            cam.lookAtDirection(new Vector3f(0f, -10f, 20f), Vector3f.UNIT_Y);*/

        } else {
            flyCam.setMoveSpeed(50);
        }
    }

    private void setDebugInfo() {
        setDisplayFps(true);
        setDisplayStatView(true);
        setPauseOnLostFocus(false);
    }

    private void initInterface() {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                viewPort);

        Nifty nifty = niftyDisplay.getNifty();
        nifty.fromXml("interfaces/score_interface.xml", "controls", this);
        if (guiViewPort.getProcessors().isEmpty())
            guiViewPort.addProcessor(niftyDisplay);
        numShotsTextview = nifty.getCurrentScreen().findElementById("numShotsText").getRenderer(TextRenderer.class);
        scoreTextview = nifty.getCurrentScreen().findElementById("scoreText").getRenderer(TextRenderer.class);
        playerChartsTextview = nifty.getCurrentScreen().findElementById("playerChartText").getRenderer(TextRenderer.class);
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

    private void createStickman() {
        stickman = new Stickman(rootNode, skeletonMap, assetManager, bulletAppState);
        /*stickman.rotateBone(2, 0, -165);
        stickman.rotateBone(4, 1, 90);
        stickman.rotateBone(4, 0, -90);*/
        System.out.println("Hand rotation:" + stickman.getBoneLocation(13).toString());
        rHandControl = stickman.getrHandControl();
    }

    private void createBall() {
        numEvent = 0;
        currTrajectory = trajectories.get(numShots);
        numShots++;
        if (ballGeometry != null) {
            rootNode.detachChild(ballGeometry);
            ballGeometry = null;
            bulletAppState.getPhysicsSpace().remove(ballPhy);
            bulletAppState.getPhysicsSpace().removeCollisionListener(this);
        }
        Sphere ballMesh = new Sphere(20, 20, 1f);
        ballGeometry = new Geometry("Ball", ballMesh);
        Material ballMat = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        ballMat.setBoolean("UseMaterialColors", true);
        ballMat.setColor("Ambient", ColorRGBA.Orange);
        ballMat.setColor("Diffuse", ColorRGBA.Orange);
        ballGeometry.setMaterial(ballMat);

        rootNode.attachChild(ballGeometry);
        ballGeometry.setLocalTranslation(-(stickman.SHOULDER_WIDTH + stickman.UARM_RADIUS * 2), 10, 2f);

        ballGeometry.setShadowMode(ShadowMode.CastAndReceive);

        HullCollisionShape ballCollShape = new HullCollisionShape(ballGeometry.getMesh());
        ballPhy = new RigidBodyControl(ballCollShape, .4f);
        ballPhy.setKinematic(false);
        ballPhy.setRestitution(2f);
        ballPhy.setFriction(100f);
        ballGeometry.addControl(ballPhy);
        bulletAppState.getPhysicsSpace().add(ballPhy);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        Vector3f direction = currTrajectory.getDirection();
        int multiplier = currTrajectory.getMultiplier();
        //System.out.println(String.format("%.2f, %.2f, %.2f, %d - ", direction.x, direction.y, direction.z, multiplier));
        //ballPhy.setLinearVelocity(direction.mult(multiplier));
        ballPhy.setLinearVelocity(new Vector3f(0, 2f, 0).mult(10));

        numShotsTextview.setText(String.format("%d/%d", numShots, MAX_SHOTS));
    }

    private void initTrajectories() {
        trajectories = TrajectoriesCreator.getDefaultTrajectories().getTrajectories();
        numShotsTextview.setText(String.format("%d/%d", numShots, MAX_SHOTS));
    }

    private void createTerrain() {
        float TERRAIN_WIDTH = 250f;
        float TERRAIN_HEIGHT = 750f;
        Quad terrainMesh = new Quad(TERRAIN_WIDTH, TERRAIN_HEIGHT);
        Geometry terrainGeometry = new Geometry("Terrain", terrainMesh);
        terrainGeometry.setLocalRotation(new Quaternion().fromAngles((float) Math.toRadians(-90), 0f, 0f));
        terrainGeometry.setLocalTranslation(-TERRAIN_WIDTH / 2, -(stickman.TORSO_HEIGHT / 2 + stickman.ULEG_LENGTH + stickman.LLEG_LENGTH), TERRAIN_HEIGHT * .75f);
        Material terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        terrainMaterial.setBoolean("UseMaterialColors", true);
        ColorRGBA sandColor = new ColorRGBA(.835f, .792f, .659f, 1f);
        terrainMaterial.setColor("Ambient", sandColor);
        terrainMaterial.setColor("Diffuse", sandColor);
        terrainGeometry.setMaterial(terrainMaterial);
        terrainGeometry.setShadowMode(ShadowMode.Receive);

        RigidBodyControl terrainPhy = new RigidBodyControl(0.0f);
        terrainGeometry.addControl(terrainPhy);
        terrainGeometry.getControl(RigidBodyControl.class).setRestitution(0.2f);
        terrainGeometry.getControl(RigidBodyControl.class).setFriction(1000f);
        bulletAppState.getPhysicsSpace().add(terrainPhy);

        rootNode.attachChild(terrainGeometry);
    }

    private void createCourt() {
        Node courtNode = new Node();

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

        Quad netSurface = new Quad(courtWidth * 2, netHeight / 2 - 1);
        Geometry netGeometry1 = new Geometry("netGeom1", netSurface);
        netGeometry1.setLocalTranslation(-courtWidth, courtLength, -netHeight);
        float[] angles1 = new float[]{(float) Math.toRadians(90), 0, 0};
        netGeometry1.setLocalRotation(new Quaternion(angles1));
        mat = new Material(assetManager,
                "Common/MatDefs/Terrain/Terrain.j3md");
        mat.setTexture("Alpha",
                assetManager.loadTexture("textures/alphamap.png"));
        Texture net = assetManager.loadTexture(
                "textures/net_texture.png");
        net.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("Tex1", net);
        mat.setFloat("Tex1Scale", 64f);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        netGeometry1.setQueueBucket(RenderQueue.Bucket.Transparent);
        netGeometry1.setMaterial(mat);

        Geometry netGeometry2 = new Geometry("netGeom2", netSurface);
        float[] angles2 = new float[]{(float) Math.toRadians(-90), 0, 0};
        netGeometry2.setLocalRotation(new Quaternion(angles2));
        netGeometry2.setLocalTranslation(-courtWidth, courtLength, -netHeight / 2 - 1);
        mat = new Material(assetManager,
                "Common/MatDefs/Terrain/Terrain.j3md");
        mat.setTexture("Alpha",
                assetManager.loadTexture("textures/alphamap.png"));
        mat.setTexture("Tex1", net);
        mat.setFloat("Tex1Scale", 64f);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        netGeometry2.setQueueBucket(RenderQueue.Bucket.Transparent);
        netGeometry2.setMaterial(mat);

        Quad pitchMesh = new Quad(courtWidth * 2, courtLength * 2);
        Geometry pitchGeometry = new Geometry("Pitch", pitchMesh);
        float[] angles3 = new float[]{0, (float) Math.toRadians(180), 0};
        pitchGeometry.setLocalRotation(new Quaternion(angles3));
        pitchGeometry.setLocalTranslation(courtWidth, 0f, 0f);
        Material pitchMaterial = new Material(assetManager,
                "Common/MatDefs/Light/Lighting.j3md");
        ColorRGBA sandColor = new ColorRGBA(.835f, .792f, .659f, 1f);
        pitchMaterial.setBoolean("UseMaterialColors", true);
        pitchMaterial.setColor("Ambient", sandColor);
        pitchMaterial.setColor("Diffuse", sandColor);
        pitchGeometry.setMaterial(pitchMaterial);

        GhostControl pitchGhostControl = new GhostControl(new MeshCollisionShape(pitchGeometry.getMesh()));
        pitchGeometry.addControl(pitchGhostControl);
        bulletAppState.getPhysicsSpace().add(pitchGhostControl);

        courtNode.attachChild(sxLineGeom);
        courtNode.attachChild(dxLineGeom);
        courtNode.attachChild(btLineGeom);
        courtNode.attachChild(tpLineGeom);
        courtNode.attachChild(leftPoleGeometry);
        courtNode.attachChild(rightPoleGeometry);
        courtNode.attachChild(lNetLineGeom);
        courtNode.attachChild(uNetLineGeom);
        courtNode.attachChild(netGeometry1);
        courtNode.attachChild(netGeometry2);
        courtNode.attachChild(pitchGeometry);
        for (Spatial child : courtNode.getChildren()) {
            child.setShadowMode(ShadowMode.Receive);
        }

        float[] rotAngles = new float[]{(float) Math.toRadians(90), 0, 0};
        Quaternion rotQuat = new Quaternion(rotAngles);
        courtNode.setLocalRotation(rotQuat);
        courtNode.setLocalTranslation(0, -(stickman.TORSO_HEIGHT / 2 + stickman.ULEG_LENGTH + stickman.LLEG_LENGTH) + 0.01f, 1f);

        rootNode.attachChild(courtNode);

        RigidBodyControl leftPolePhy = new RigidBodyControl(0.0f);
        leftPoleGeometry.addControl(leftPolePhy);
        bulletAppState.getPhysicsSpace().add(leftPolePhy);

        RigidBodyControl rightPolePhy = new RigidBodyControl(0.0f);
        rightPoleGeometry.addControl(rightPolePhy);
        bulletAppState.getPhysicsSpace().add(rightPolePhy);

        RigidBodyControl netPhy1 = new RigidBodyControl(0.0f);
        netGeometry1.addControl(netPhy1);
        bulletAppState.getPhysicsSpace().add(netPhy1);

        RigidBodyControl netPhy2 = new RigidBodyControl(0.0f);
        netGeometry2.addControl(netPhy2);
        bulletAppState.getPhysicsSpace().add(netPhy2);
    }

    private void createTargets() {

        float targetSize = 10f;
        float targetHeightOffset = 0.4f;

        for (int i = 0; i < 42; i++) {
            ColorRGBA color;
            switch (i) {
                case 8:
                case 12:
                case 29:
                case 33:
                case 37:
                case 38:
                case 39:
                    color = ColorRGBA.Red;
                    break;
                case 9:
                case 10:
                case 11:
                case 15:
                case 19:
                case 22:
                case 26:
                    color = ColorRGBA.Orange;
                    break;
                case 16:
                case 17:
                case 18:
                case 23:
                case 24:
                case 25:
                case 30:
                case 31:
                case 32:
                    color = ColorRGBA.Magenta;
                    break;
                case 36:
                case 40:
                    color = ColorRGBA.Cyan;
                    break;
                default:
                    color = ColorRGBA.LightGray;
                    break;
            }
            Quad targetMesh = new Quad(targetSize, targetSize);
            Geometry target = new Geometry(String.format("Target%d", i), targetMesh);
            target.setLocalRotation(new Quaternion().fromAngles((float) Math.toRadians(-90), 0f, 0f));
            float xOffset = -(targetSize * (i % 7));
            float zOffset = targetSize * (-(i / 7) + 1);
            target.setLocalTranslation(courtWidth + xOffset,
                    -(stickman.TORSO_HEIGHT / 2 + stickman.ULEG_LENGTH + stickman.LLEG_LENGTH) + targetHeightOffset,
                    courtLength * 2 + zOffset);
            Material targetMaterial = new Material(assetManager,
                    "Common/MatDefs/Light/Lighting.j3md");
            targetMaterial.setBoolean("UseMaterialColors", true);
            targetMaterial.setColor("Ambient", color);
            targetMaterial.setColor("Diffuse", color);
            target.setMaterial(targetMaterial);
            target.setShadowMode(ShadowMode.Receive);
            rootNode.attachChild(target);

            GhostControl targetGhostControl = new GhostControl(new MeshCollisionShape(target.getMesh()));
            target.addControl(targetGhostControl);
            bulletAppState.getPhysicsSpace().add(targetGhostControl);
        }
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

    private void initKeys() {
        inputManager.deleteMapping("ResetCamera");
        inputManager.addMapping("TossBall", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("ResetCamera", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("ResetGame", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(actionListener, "TossBall");
        inputManager.addListener(actionListener, "ResetCamera");
        inputManager.addListener(actionListener, "ResetGame");

    }

    private void resetGame() {
        //TODO save score and update charts
        score = 0;
        numShots = 0;
        numShotsTextview.setText(String.format("%d/%d", numShots, MAX_SHOTS));
        scoreTextview.setText(String.format("%d", score));
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        String nameA = event.getNodeA().getName();
        String nameB = event.getNodeB().getName();
        if (nameA.equals("rHandGeometry") && nameB.equals("Ball")) {
            if (numEvent == 0) {
                numEvent++;
                final Spatial hand = event.getNodeA();
                final Spatial ball = event.getNodeB();
                hitBall(event, ball, hand);
            }
        } else if (nameB.equals("rHandGeometry") && nameA.equals("Ball")) {
            if (numEvent == 0) {
                numEvent++;
                final Spatial hand = event.getNodeB();
                final Spatial ball = event.getNodeA();
                hitBall(event, ball, hand);
            }
        } else if (nameA.startsWith("Target") && nameB.equals("Ball")) {
            if (numEvent == 0) {
                numEvent++;
                System.out.println(nameA);
                hitTarget(Integer.parseInt(nameA.replace("Target", "")));
            }
        } else if (nameB.startsWith("Target") && nameA.equals("Ball")) {
            if (numEvent == 0) {
                numEvent++;
                System.out.println(nameB);
                hitTarget(Integer.parseInt(nameB.replace("Target", "")));
            }
        } else if (nameA.equals("Pitch") && nameB.equals("Ball")) {
            if (numEvent == 0) {
                numEvent++;
                //hitTarget(1);
                System.out.println("Pitch");
            }
        } else if (nameB.equals("Pitch") && nameA.equals("Ball")) {
            if (numEvent == 0) {
                numEvent++;
                //hitTarget(1);
                System.out.println("Pitch");
            }
        }
    }

    private void hitBall(PhysicsCollisionEvent event, Spatial ball, Spatial hand) {
        Vector3f handPosition = rHandControl.getPhysicsLocation();
        Vector3f ballPosition = ballPhy.getPhysicsLocation();
        Vector3f tmp = ballPosition.subtract(handPosition);
        float minDist = Float.MAX_VALUE;
        Trajectory minTrajectory = null;
        for (Trajectory trajectory : trajectories) {
            float dist = trajectory.getDirection().distance(tmp);
            if (dist < minDist) {
                minDist = dist;
                minTrajectory = trajectory;
            }
        }
        if (minTrajectory != null) {
            ballPhy.setLinearVelocity(minTrajectory.getDirection().mult(minTrajectory.getMultiplier()));
            numEvent = 0;
        }
    }

    private void hitTarget(int target) {
        int targetScore;
        switch (target) {
            case 8:
            case 12:
            case 29:
            case 33:
            case 37:
            case 38:
            case 39:
                targetScore = 8;
                break;
            case 9:
            case 10:
            case 11:
            case 15:
            case 19:
            case 22:
            case 26:
                targetScore = 6;
                break;
            case 16:
            case 17:
            case 18:
            case 23:
            case 24:
            case 25:
            case 30:
            case 31:
            case 32:
                targetScore = 4;
                break;
            case 36:
            case 40:
                targetScore = 10;
                break;
            default:
                targetScore = 0;
                break;
        }
        score += targetScore;
        scoreTextview.setText(String.format("%d", score));
        //createBall();
    }

    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen) {

    }

    @Override
    public void onStartScreen() {

    }

    @Override
    public void onEndScreen() {

    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {

    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
        /*elapsedTime += tpf;
        if (elapsedTime >= 0.05) {
            Vector3f currentPosition = rHandControl.getPhysicsLocation();
            if (prevHandPosition != null) {
                prevPos.add(currentPosition);
                elapsedTime = 0;
            }
            prevHandPosition = currentPosition;
        }*/
    }
}
