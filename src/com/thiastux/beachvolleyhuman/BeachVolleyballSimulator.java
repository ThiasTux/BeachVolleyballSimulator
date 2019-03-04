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
import com.thiastux.beachvolleyhuman.model.*;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.render.PanelRenderer;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.tools.Color;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import javax.annotation.Nonnull;
import java.util.*;

public class BeachVolleyballSimulator extends SimpleApplication implements PhysicsTickListener, ScreenController {

    private static int MAX_SHOTS = 15;
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
    private int numBallEvent;
    private int numTargetEvent;
    private RigidBodyControl rHandControl;
    private PanelRenderer currScorePanel;
    private TextRenderer currScoreTextview;
    private TextRenderer numShotsTextview;
    private TextRenderer scoreTextview;
    private TextRenderer playerChartsTextview;
    private TextRenderer playerNameTextview;

    private int numShots = 0;
    private int score = 0;
    private float elapsedTime = 0;
    private Vector3f prevPosition;
    private Vector3f currPosition;
    private CircularFifoQueue<Vector3f> prevHandPositions = new CircularFifoQueue<>(4);
    private float courtWidth = 25f;
    private float courtLength = courtWidth * 2;
    private float lineWidth = 0.2f;
    private float lineThickness = 0.01f;
    private float netHeight = 16f;
    private float poleThickness = 0.3f;
    private List<Trajectory> trajectories;
    private boolean isFirstLaunch = true;
    private TextField playerNameTextfield;
    private String playerName;
    private NiftyJmeDisplay niftyDisplay;
    private boolean scoringEnabled = false;
    private boolean gameStarted = false;
    private LogService logService;
    private List<Score> scores;
    private PhysicsCollisionListener targetCollisionListener = new PhysicsCollisionListener() {
        @Override
        public void collision(PhysicsCollisionEvent event) {
            String nameA = event.getNodeA().getName();
            String nameB = event.getNodeB().getName();
            if (nameA.startsWith("Target") && nameB.equals("Ball")) {
                if (numTargetEvent == 0) {
                    numTargetEvent++;
                    System.out.println(String.format("%s\t%s", nameA, scoringEnabled));
                    hitTarget(Integer.parseInt(nameA.replace("Target", "")));
                }
            } else if (nameB.startsWith("Target") && nameA.equals("Ball")) {
                if (numTargetEvent == 0) {
                    numTargetEvent++;
                    System.out.println(String.format("%s\t%s", nameB, scoringEnabled));
                    hitTarget(Integer.parseInt(nameB.replace("Target", "")));
                }
            } else if (nameA.equals("Pitch") && nameB.equals("Ball")) {
                if (numTargetEvent == 0) {
                    numTargetEvent++;
                    //hitTarget(1);
                    System.out.println("Pitch");
                }
            } else if (nameB.equals("Pitch") && nameA.equals("Ball")) {
                if (numTargetEvent == 0) {
                    numTargetEvent++;
                    //hitTarget(1);
                    System.out.println("Pitch");
                }
            }
        }
    };
    private PhysicsCollisionListener ballCollisionListener = new PhysicsCollisionListener() {
        @Override
        public void collision(PhysicsCollisionEvent event) {
            String nameA = event.getNodeA().getName();
            String nameB = event.getNodeB().getName();
            if ((nameA.equals("rHandGeometry") && nameB.equals("Ball") ||
                    (nameB.equals("rHandGeometry") && nameA.equals("Ball")))) {
                if (numBallEvent == 0) {
                    numBallEvent++;
                    prevPosition = prevHandPositions.peek();
                    currPosition = prevHandPositions.get(3);
                    hitBall();
                }
            }
        }
    };
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


    private BeachVolleyballSimulator(String[] args) {
        tcpDataClient = new TCPDataClient(this, args);
    }

    public static void main(String[] args) {

        DEBUG = Boolean.parseBoolean(args[0]);

        BeachVolleyballSimulator app = new BeachVolleyballSimulator(Arrays.copyOfRange(args, 3, args.length));
        app.setShowSettings(false);
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1920);
        settings.setHeight(1050);
        settings.setSamples(16);
        //settings.setVSync(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        initPhysics();
        setCamera();
        setDebugInfo();
        addReferenceSystem();
        createStickman();
        createTerrain();
        createCourt();
        createTargets();
        setLight();
        computeInitialQuaternions();
        initTrajectories();
        resetGame();
        initLogService();

        tcpDataClient.startExecution();

    }

    @Override
    public void simpleUpdate(float tpf) {
        elapsedTime += tpf;
        boolean animStart = Const.animationStart;
        if (animStart) {
            getData();
            animateModel();
        }
    }

    @Override
    public void stop() {
        if (!DEBUG) {
            tcpDataClient.stopExecution();
        }
        scores.add(new Score(playerName, score, numShots, MAX_SHOTS));
        scores.sort(Score.comparator);
        logService.saveScores(scores);
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
        outputQuat = new Quaternion(-outputQuat.getX(), outputQuat.getZ(), outputQuat.getY(), outputQuat.getW());
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
        //Quaternion quat1 = new Quaternion().fromAngles(0f, 0f, (float) Math.toRadians(90));
        //preRot = new Quaternion().fromAngles((float) Math.toRadians(90), 0f, 0f);
        preRot = new Quaternion().fromAngles(0f, 0f, (float) Math.toRadians(-180));
        //preRot = quat1.mult(quat2);

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
        setDisplayFps(false);
        setDisplayStatView(false);
        setPauseOnLostFocus(false);
    }

    private void initLogService() {
        logService = new LogService();
        scores = logService.readScoresFromFile();
        if (scores == null)
            scores = new ArrayList<>();
    }

    private void initInterface(int resetStatus) {
        if (niftyDisplay == null)
            niftyDisplay = new NiftyJmeDisplay(assetManager,
                    inputManager,
                    audioRenderer,
                    viewPort);
        if (guiViewPort.getProcessors().isEmpty())
            guiViewPort.addProcessor(niftyDisplay);

        Nifty nifty = niftyDisplay.getNifty();
        switch (resetStatus) {
            case 0:
                nifty.fromXml("interfaces/player_name_interface.xml", "popupScreen", this);
                nifty.update();
                playerNameTextfield = nifty.getCurrentScreen().findNiftyControl("userLabel", TextField.class);
                break;
            case 1:
                nifty.fromXml("interfaces/score_interface.xml", "controls", this);
                nifty.update();
                numShotsTextview = nifty.getCurrentScreen().findElementById("numShotsText").getRenderer(TextRenderer.class);
                scoreTextview = nifty.getCurrentScreen().findElementById("scoreText").getRenderer(TextRenderer.class);
                playerChartsTextview = nifty.getCurrentScreen().findElementById("playerChartText").getRenderer(TextRenderer.class);
                playerNameTextview = nifty.getCurrentScreen().findElementById("playerNameText").getRenderer(TextRenderer.class);
                currScorePanel = nifty.getCurrentScreen().findElementById("currScorePanel").getRenderer(PanelRenderer.class);
                currScoreTextview = nifty.getCurrentScreen().findElementById("currScoreText").getRenderer(TextRenderer.class);
                numShotsTextview.setText(String.format("%d/%d", numShots, MAX_SHOTS));
                scoreTextview.setText(String.format("%d", score));
                playerNameTextview.setText(String.format("%s", playerName));
                resetScoringFeedback();
                String scoreCharts = "";
                scores.sort(Score.comparator);
                for (Score score : scores.subList(0, scores.size() >= 11 ? 10 : scores.size())) {
                    scoreCharts += String.format("%d %s\n", score.getScore(), score.getPlayerName());
                }
                playerChartsTextview.setText(scoreCharts);
                break;
            case 2:
                nifty.fromXml("interfaces/end_game_interface.xml", "popupScreen", this);
                nifty.update();
                break;
        }
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

        refNode.setLocalTranslation(0, netHeight / 2 - 1, courtLength);

        rootNode.attachChild(refNode);
    }

    private void createStickman() {
        stickman = new Stickman(rootNode, skeletonMap, assetManager, bulletAppState);
        System.out.println("Hand rotation:" + stickman.getBoneLocation(13).toString());
        rHandControl = stickman.getrHandControl();
    }

    private void createBall() {
        if (gameStarted) {
            if (numShots < MAX_SHOTS) {
                numShots++;
                if (ballGeometry != null) {
                    rootNode.detachChild(ballGeometry);
                    ballGeometry = null;
                    bulletAppState.getPhysicsSpace().remove(ballPhy);
                    bulletAppState.getPhysicsSpace().removeCollisionListener(ballCollisionListener);
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
                ballGeometry.setLocalTranslation(-(stickman.SHOULDER_WIDTH + stickman.UARM_RADIUS * 2) - 1, 10, 2.2f);

                ballGeometry.setShadowMode(ShadowMode.CastAndReceive);

                HullCollisionShape ballCollShape = new HullCollisionShape(ballGeometry.getMesh());
                ballPhy = new RigidBodyControl(ballCollShape, .4f);
                ballPhy.setKinematic(false);
                ballPhy.setRestitution(2f);
                ballPhy.setFriction(100f);
                ballGeometry.addControl(ballPhy);
                bulletAppState.getPhysicsSpace().add(ballPhy);
                bulletAppState.getPhysicsSpace().addCollisionListener(ballCollisionListener);
                ballPhy.setLinearVelocity(new Vector3f(0, 2.2f, 0).mult(10));
                bulletAppState.getPhysicsSpace().addCollisionListener(targetCollisionListener);
                numShotsTextview.setText(String.format("%d/%d", numShots, MAX_SHOTS));
                numBallEvent = 0;
            } else {
                endGame();
            }
        }
    }

    private void initTrajectories() {
        trajectories = TrajectoriesCreator.getDefaultTrajectories().getTrajectories();
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

        for (int i = 0; i < 25; i++) {
            ColorRGBA color;
            switch (i) {
                case 0:
                case 4:
                case 15:
                case 19:
                case 21:
                case 22:
                case 23:
                    color = ColorRGBA.Blue;
                    break;
                case 1:
                case 2:
                case 3:
                case 5:
                case 9:
                case 10:
                case 14:
                    color = ColorRGBA.Yellow;
                    break;
                case 6:
                case 7:
                case 8:
                case 11:
                case 12:
                case 13:
                case 16:
                case 17:
                case 18:
                    color = ColorRGBA.Magenta;
                    break;
                case 20:
                case 24:
                    color = ColorRGBA.Green;
                    break;
                default:
                    color = ColorRGBA.LightGray;
                    break;
            }
            Quad targetMesh = new Quad(targetSize * 0.9f, targetSize * 0.9f);
            Geometry target = new Geometry(String.format("Target%d", i), targetMesh);
            target.setLocalRotation(new Quaternion().fromAngles((float) Math.toRadians(-90), 0f, 0f));
            float xOffset = -(targetSize * ((i % 5) + 1)) + 0.5f;
            float zOffset = targetSize * (-(i / 5));
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
        inputManager.addMapping("TossBall", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("ResetCamera", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("ResetGame", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(actionListener, "TossBall");
        inputManager.addListener(actionListener, "ResetCamera");
        inputManager.addListener(actionListener, "ResetGame");
    }

    private void resetKeys() {
        inputManager.deleteMapping("TossBall");
        inputManager.deleteMapping("ResetCamera");
        inputManager.deleteMapping("ResetGame");
    }

    public void startGame() {
        playerName = playerNameTextfield.getDisplayedText();
        guiViewPort.removeProcessor(niftyDisplay);
        score = 0;
        numShots = 0;
        initInterface(Const.START_GAME);
        gameStarted = true;
        initKeys();
    }

    public void resetGame() {
        resetKeys();
        gameStarted = false;
        initInterface(Const.RESET_GAME);
        if (!isFirstLaunch) {
            scores.add(new Score(playerName, score, numShots, MAX_SHOTS));
        }
        isFirstLaunch = false;
    }

    public void endGame() {
        resetKeys();
        gameStarted = false;
        initInterface(Const.END_GAME);
    }

    private void resetScoringFeedback() {
        currScorePanel.setBackgroundColor(Color.NONE);
        currScoreTextview.setColor(Color.NONE);
    }

    private void setScoringFeedback(Color feedbackBackground, Color feedbackForeground, int score) {
        currScorePanel.setBackgroundColor(feedbackBackground);
        currScoreTextview.setColor(feedbackForeground);
        currScoreTextview.setText(String.valueOf(score));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                resetScoringFeedback();
            }
        }, 3000);
    }

    private void hitBall() {
        bulletAppState.getPhysicsSpace().addCollisionListener(targetCollisionListener);
        numTargetEvent = 0;
        Vector3f tmp = ballPhy.getPhysicsLocation().subtract(prevPosition);
        float distance = currPosition.distance(prevPosition);
        System.out.println(distance);
        float minDist = Float.MAX_VALUE;
        Trajectory minTrajectory = null;
        tmp = tmp.mult(distance);
        for (Trajectory trajectory : trajectories) {
            float dist = trajectory.getDirection().distance(tmp);
            if (dist < minDist) {
                minDist = dist;
                minTrajectory = trajectory;
            }
        }
        if (minTrajectory != null) {
            System.out.println(minTrajectory.getDirection().toString());
            System.out.println("MinDist " + minDist);
            ballPhy.setLinearVelocity(minTrajectory.getDirection().mult(minTrajectory.getMultiplier()));
            scoringEnabled = false;
        }
    }

    private void hitTarget(int target) {
        if (scoringEnabled) {
            scoringEnabled = false;
            int targetScore;
            Color feedbackBackground;
            Color feedbackForeground = Color.WHITE;
            switch (target) {
                case 0:
                case 4:
                case 15:
                case 19:
                case 21:
                case 22:
                case 23:
                    feedbackBackground = new Color("#00ff");
                    targetScore = 8;
                    break;
                case 1:
                case 2:
                case 3:
                case 5:
                case 9:
                case 10:
                case 14:
                    feedbackBackground = new Color("#ff0f");
                    feedbackForeground = Color.BLACK;
                    targetScore = 6;
                    break;
                case 6:
                case 7:
                case 8:
                case 11:
                case 12:
                case 13:
                case 16:
                case 17:
                case 18:
                    feedbackBackground = new Color("#f0ff");
                    targetScore = 4;
                    break;
                case 20:
                case 24:
                    feedbackBackground = new Color("#0f0f");
                    feedbackForeground = Color.BLACK;
                    targetScore = 10;
                    break;
                default:
                    feedbackBackground = Color.NONE;
                    feedbackForeground = Color.NONE;
                    targetScore = 0;
                    break;
            }
            score += targetScore;
            scoreTextview.setText(String.format("%d", score));
            setScoringFeedback(feedbackBackground, feedbackForeground, targetScore);
        }
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
        if (numTargetEvent >= 1)
            bulletAppState.getPhysicsSpace().removeCollisionListener(targetCollisionListener);
        if (numBallEvent >= 1)
            bulletAppState.getPhysicsSpace().removeCollisionListener(ballCollisionListener);
        if (numBallEvent == 0) {
            elapsedTime += tpf;
            if (elapsedTime >= 0.125)
                prevHandPositions.add(rHandControl.getPhysicsLocation());
        }
    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
        if (ballPhy != null) {
            Vector3f ballPosition = ballGeometry.getLocalTranslation();
            if (!scoringEnabled && ballPosition.z > courtLength / 2 + 1 && ballPosition.y > netHeight / 2 - 1) {
                System.out.println("z " + ballPosition.z);
                System.out.println("y " + ballPosition.y);
                scoringEnabled = true;
            }
        }
    }
}
