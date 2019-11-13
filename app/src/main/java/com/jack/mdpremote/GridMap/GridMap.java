package com.jack.mdpremote.GridMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jack.mdpremote.MainActivity;
import com.jack.mdpremote.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class GridMap extends View {

    private static final String TAG = "GridMap";
    private static final int COL = 15, ROW = 20;
    private static float cellSize;      // indicating the cell size
    private static JSONObject receivedJsonObject = new JSONObject();    // for storing the current map information
    private static JSONObject mapInformation;           // for creating a placeholder information and to send information to MapInformation.class
    private static JSONObject backupMapInformation;     // for saving a copy of the received map information
    private static Cell[][] cells;      // for creating cells
    private static String robotDirection = "None";      // indicate the current direction of the robot
    private static int[] startCoord = new int[]{-1, -1};       // 0: col, 1: row
    private static int[] curCoord = new int[]{-1, -1};         // 0: col, 1: row
    private static int[] oldCoord = new int[]{-1, -1};         // 0: col, 1: row
    private static int[] waypointCoord = new int[]{-1, -1};    // 0: col, 1: row
    private static ArrayList<String[]> imageCoord = new ArrayList<>(); // storing all image coordinates
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>(); // storing all obstacles coordinate
    private static boolean autoUpdate = false;          // false: manual mode, true: auto mode
    private static boolean mapDrawn = false;            // false: map not drawn, true: map drawn
    private static boolean canDrawRobot = false;        // false: cannot draw robot, true: can draw robot
    private static boolean setWaypointStatus = false;   // false: cannot set waypoint, true: can set waypoint
    private static boolean startCoordStatus = false;    // false: cannot set starting point, true: can set starting point
    private static boolean setObstacleStatus = false;   // false: cannot set obstacle, true: can set obstacle
    private static boolean unSetCellStatus = false;     // false: cannot unset cell, true: can unset cell
    private static boolean setExploredStatus = false;   // false: cannot check cell, true: can check cell
    private static boolean validPosition = false;       // false: robot out of range, true: robot within range
    private Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_error);   // default image for bitmap

    private Paint paintBlack = new Paint();         // for lines, etc
    private Paint paintWhite = new Paint();         // for lines, etc
    private Paint obstacleColor = new Paint();      // black = obstacles position
    private Paint robotColor = new Paint();         // cyan = robot position
    private Paint endColor = new Paint();           // red = end position
    private Paint startColor = new Paint();         // green = start position
    private Paint waypointColor = new Paint();      // yellow = waypoint position
    private Paint unexploredColor = new Paint();    // gray = unexplored position
    private Paint exploredColor = new Paint();      // white = explored position
    private Paint imageColor = new Paint();         // blue = image front position
    private Paint fastestPathColor = new Paint();   // magenta = fastest path position

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    // constructor of grid map
    public GridMap(Context context) {
        super(context);
        init(null);
    }

    // constructor of grid map
    public GridMap(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);

        paintWhite.setColor(Color.WHITE);   // for lines, etc
        paintBlack.setStyle(Paint.Style.FILL_AND_STROKE);   // for lines, etc
        obstacleColor.setColor(Color.BLACK);                // black = obstacles position
        robotColor.setColor(Color.CYAN);                   // black = robot position
        endColor.setColor(Color.GREEN);                       // dark green = end position
        startColor.setColor(Color.CYAN);                    // green = start position
        waypointColor.setColor(Color.YELLOW);               // yellow = waypoint position
        unexploredColor.setColor(Color.GRAY);               // black = unexplored position
        exploredColor.setColor(Color.WHITE);                // white = explored position
        imageColor.setColor(Color.BLACK);                   // black = image position
        fastestPathColor.setColor(Color.MAGENTA);           // magenta = fastest path position
    }

    // nullable allows parameter, field or method return value to be null if needed
    private void init(@Nullable AttributeSet attrs) {
        setWillNotDraw(false);
    }

    // to convert from android coordinate to screen coordinate, vice versa
    private int convertRow(int row) {
        return (20 - row);
    }

    // draw the custom view grid map
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        ArrayList<String[]> imageCoord = this.getImageCoord();
        int[] curCoord = this.getCurCoord();
        
        if (!this.getMapDrawn()) {
            canvas.drawColor(Color.parseColor("#000000"));
            // create placeholder for image coordinate
            String[] placeholderImageCoord = new String[3];
            placeholderImageCoord[0] = "999";
            placeholderImageCoord[1] = "999";
            placeholderImageCoord[2] = "placeholder";
            imageCoord.add(placeholderImageCoord);
            // create cell only when launching the application
            this.createCell();
            // set ending coordinate
            this.setEndCoord(14, 19);
            mapDrawn = true;
        }

        // draw individual cell
        this.drawIndividualCell(canvas);
        // draw grid number
        this.drawGridNumber(canvas);
        // draw robot position
        if (this.getCanDrawRobot())
            this.drawRobot(canvas, curCoord);
        // draw image position
        this.drawImages(canvas, imageCoord);

    }

    // initialise cell
    private void createCell() {
        showLog("Creating Cell");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++)
            for (int y = 0; y <= ROW; y++)
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
    }

    // set auto update
    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        showLog(String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    // get auto update
    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    // get message received status
    public boolean getMapDrawn() {
        return mapDrawn;
    }

    // set valid position status
    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    // get valid position status
    public boolean getValidPosition() {
        return validPosition;
    }

    // set unset cell status
    public void setUnSetCellStatus(boolean status) {
        unSetCellStatus = status;
    }

    // get unset cell status
    public boolean getUnSetCellStatus() {
        return unSetCellStatus;
    }

    // set set obstacle status
    public void setSetObstacleStatus(boolean status) {
        setObstacleStatus = status;
    }

    // get set obstacle status
    public boolean getSetObstacleStatus() {
        return setObstacleStatus;
    }

    // get explored cell status
    public void setExploredStatus(boolean status) {
        setExploredStatus = status;
    }

    // get set obstacle status
    public boolean getExploredStatus() {
        return setExploredStatus;
    }


    // set start coordinate status
    public void setStartCoordStatus(boolean status) {
        startCoordStatus = status;
    }

    // get start coordinate status
    private boolean getStartCoordStatus() {
        return startCoordStatus;
    }

    // set way point status
    public void setWaypointStatus(boolean status) {
        setWaypointStatus = status;
    }

    // get can draw robot boolean value
    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    // set ending coordinates
    public void setEndCoord(int col, int row) {
        showLog("Entering setEndCoord");
        //convert to android coordinate
        row = this.convertRow(row);
        // change the color of ending coordinate
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("end");
        showLog("Exiting setEndCoord");
    }

    // set starting coordinates
    public void setStartCoord(int col, int row) {
        showLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;

        // if starting coordinate not set
        if (this.getStartCoordStatus())
            // convert to android coordinate
            this.setCurCoord(col, row, "right");
        showLog("Exiting setStartCoord");
    }

    // get starting coordinates (for auto/manual)
    private int[] getStartCoord() {
        return startCoord;
    }

    // set robot current coordinates
    public void setCurCoord(int col, int row, String direction) {
        showLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxis(col-1, row-1, direction);

        // convert to android coordinate
        row = this.convertRow(row);
        // change the color of robot current coordinate
        for (int x = col - 1; x <= col + 1; x++)
            for (int y = row - 1; y <= row + 1; y++)
                cells[x][y].setType("robot");
        showLog("Exiting setCurCoord");
    }

    // for updating the text view when robot changes it's current coordinates
    private void updateRobotAxis(int col, int row, String direction) {
        // for updating the x-axis, y-axis and direction axis (for auto mode)
        TextView xAxisTextView = ((Activity) this.getContext()).findViewById(R.id.xAxisTextView);
        TextView yAxisTextView = ((Activity) this.getContext()).findViewById(R.id.yAxisTextView);
//        Spinner directionDropdown = ((Activity) this.getContext()).findViewById(R.id.directionDropdown);
//        switch (direction){
//            case "None":
//                directionDropdown.setSelection(0);
//                break;
//            case "up":
//                directionDropdown.setSelection(1);
//                break;
//            case "down":
//                directionDropdown.setSelection(2);
//                break;
//            case "left":
//                directionDropdown.setSelection(3);
//                break;
//            case "right":
//                directionDropdown.setSelection(4);
//                break;
//        }
        xAxisTextView.setText(String.valueOf(col));
        yAxisTextView.setText(String.valueOf(row));


    }

    // get current coordinate
    public int[] getCurCoord() {
        // screen coordinate
        return curCoord;
    }

    // set direction of the robot
    public void setRobotDirection(String direction) {
        this.sharedPreferences();
        robotDirection = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    // get direction of the robot
    public String getRobotDirection() {
        return robotDirection;
    }

    // set waypoint coordinate
    private void setWaypointCoord(int col, int row) throws JSONException {
        showLog("Entering setWaypointCoord");
        waypointCoord[0] = col;
        waypointCoord[1] = row;

        // convert to android coordinate
        row = this.convertRow(row);
        cells[col][row].setType("waypoint");

        String wp_x= ""+ (waypointCoord[0]-1);
        String wp_y =""+ (waypointCoord[1]-1);

        if ((waypointCoord[0]-1)<10){
            wp_x = "0" + (waypointCoord[0]-1);
        }

        if ((waypointCoord[1]-1)<10)
            wp_y = "0" + (waypointCoord[1]-1);

        MainActivity.setSPWP("1", wp_x, wp_y);
        showLog("Exiting setWaypointCoord");
    }

    // get waypoint coordinate
    private int[] getWaypointCoord() {
        // screen coordinate
        return waypointCoord;
    }

    // set obstacle coordinate
    private void setObstacleCoord(int col, int row) {
        showLog("Setting obstacle coordinates");

        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);

        row = this.convertRow(row);

        cells[col][row].setType("obstacle");

    }

    // get obstacle coordinate (screen coordinate)
    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    // move robot coordinate
    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);  // reset it to default value
        int[] curCoord = this.getCurCoord();                        // screen coordinate
        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();   // screen coordinate
        this.setOldRobotCoord(curCoord[0], curCoord[1]);            // screen coordinate
        int[] oldCoord = this.getOldRobotCoord();                   // screen coordinate
        String robotDirection = getRobotDirection();
        String backupDirection = robotDirection;

        // to move robot if validPosition is true
        switch (robotDirection) {
            case "up":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "right";
                        break;
                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "left";
                        break;
                    default:
                        robotDirection = "error up";
                        break;
                }
                break;
            case "right":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "down";
                        break;
                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "up";
                        break;
                    default:
                        robotDirection = "error right";
                }
                break;
            case "down":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "left";
                        break;
                    case "back":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "right";
                        break;
                    default:
                        robotDirection = "error down";
                }
                break;
            case "left":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "right":
                        robotDirection = "up";
                        break;
                    case "back":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "down";
                        break;
                    default:
                        robotDirection = "error left";
                }
                break;
            default:
                robotDirection = "error moveCurCoord";
                break;
        }
        // update on current coordinate and robot direction
        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    // set old robot coordinate
    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        // convert to android coordinate
        oldRow = this.convertRow(oldRow);
        // change the color of robot current coordinate
        for (int x = oldCol - 1; x <= oldCol + 1; x++)
            for (int y = oldRow - 1; y <= oldRow + 1; y++)
                cells[x][y].setType("explored");
        showLog("Exiting setOldRobotCoord");
    }

    // get old robot coordinate
    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    // set image coordinate
    private void setImageCoordinate(int col, int row, String imageType) {
        col+=1;
        row+=1;
        String[] imageCoord = new String[3];
        imageCoord[0] = String.valueOf(col);
        imageCoord[1] = String.valueOf(row);
        imageCoord[2] = imageType;

        boolean update = true;

        // Check if there is already a current image coord printed out
        for (int i = 0; i < this.getImageCoord().size(); i++)
            if (this.getImageCoord().get(i)[0].equals(imageCoord[0]) && this.getImageCoord().get(i)[1].equals(imageCoord[1]) && this.getImageCoord().get(i)[1].equals(imageCoord[1]))
                update = false;

        if (!update)
            showLog("false");

        // Check if image coord is new, add the new image and print out the coord
        if (update) {
            showLog("Cell type: " + cells[col][20-row].type);
            if (cells[col][20-row].type.equals("obstacle")) {

                try {

                    this.getImageCoord().add(imageCoord);
                    this.sharedPreferences();
                    String message = "(" + (col - 1) + ", " + (row - 1) + ", " + Integer.parseInt(imageCoord[2],16)  + ")";
                    editor.putString("image", sharedPreferences.getString("image", "") + "\n " + message);
                    editor.commit();
                    showLog("Creating image: " + message);
                    row = convertRow(row);
                    cells[col][row].setType("image");

                } catch (Exception e){

                    showLog("Error creating image: " + e);

                }

            }
        }

    }

    // get image coordinate (screen coordinate)
    private ArrayList<String[]> getImageCoord() {
        return imageCoord;
    }

    // draw individual cell
    private void drawIndividualCell(Canvas canvas) {
        showLog("Entering drawIndividualCell");
        for (int x = 1; x <= COL; x++)
            for (int y = 0; y < ROW; y++)
                for (int i = 0; i < this.getImageCoord().size(); i++)
                    canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);

        showLog("Exiting drawIndividualCell");
    }

    // draw grid number on grid map
    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        // draw x-axis number
        for (int x = 1; x <= COL; x++) {
            // for 2 digit number
            if (x > 10)
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 3), paintWhite);
            else
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 3), cells[x][20].startY + (cellSize / 3), paintWhite);
        }
        // draw y-axis number
        for (int y = 0; y < ROW; y++) {
            // for 2 digit number
            if ((20 - (y + 1)) > 9)
                canvas.drawText(Integer.toString(20 - (y + 1)), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), paintWhite);
            else
                canvas.drawText(Integer.toString(20 - (y + 1)), cells[0][y].startX + (cellSize / 1.5f), cells[0][y].startY + (cellSize / 1.5f), paintWhite);
        }
        showLog("Exiting drawGridNumber");
    }

    // draw robot position
    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        // convert to android coordinate
        int androidRowCoord = this.convertRow(curCoord[1]);
        // remove horizontal lines for robot
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++)
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        // remove vertical lines for robot
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++)
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);

        // draw robot shape
        switch (this.getRobotDirection()) {
            case "up":
                // draw from bottom left to top center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, paintBlack);
                // draw from top center to bottom right
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, paintBlack);
                break;
            case "down":
                // draw from top left to bottom center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, paintBlack);
                // draw from bottom center to top right
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, paintBlack);
                break;
            case "right":
                // draw from top left to right center
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, paintBlack);
                // draw from right center to bottom left
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, paintBlack);
                break;
            case "left":
                // draw from top right to left center
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, paintBlack);
                // draw from left center to bottom right
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, paintBlack);
                break;
            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    // draw the images on the respective coordinate
    private void drawImages(Canvas canvas, ArrayList<String[]> imageCoord) {
        showLog("Drawing images");
        // RectF holds four float coordinates for a rectangle (left, top, right, bottom)
        RectF rect;

        for (int i = 0; i < imageCoord.size(); i++) {
            if (!imageCoord.get(i)[2].equals("placeholder")) {
                // convert to android coordinate
                int col = Integer.parseInt(imageCoord.get(i)[0]);
                int row = convertRow(Integer.parseInt(imageCoord.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);

                switch (imageCoord.get(i)[2]) {
                    case "1":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.up);
                        break;
                    case "2":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.down);
                        break;
                    case "3":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.right);
                        break;
                    case "4":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.left);
                        break;
                    case "5":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.circle);
                        break;
                    case "6":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.one);
                        break;
                    case "7":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.two);
                        break;
                    case "8":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.three);
                        break;
                    case "9":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.four);
                        break;
                    case "A":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.five);
                        break;
                    case "B":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.letter_a);
                        break;
                    case "C":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.letter_b);
                        break;
                    case "D":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.letter_c);
                        break;
                    case "E":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.letter_d);
                        break;
                    case "F":
                        imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.letter_e);
                        break;
                    default:
                        break;
                }
                canvas.drawBitmap(imageBitmap, null, rect, null);
            }

        }
    }

    // cell class
    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;
                case "robot":
                    this.paint = robotColor;
                    break;
                case "end":
                    this.paint = endColor;
                    break;
                case "start":
                    this.paint = startColor;
                    break;
                case "waypoint":
                    this.paint = waypointColor;
                    break;
                case "unexplored":
                    this.paint = unexploredColor;
                    break;
                case "explored":
                    this.paint = exploredColor;
                    break;
                case "image":
                    this.paint = imageColor;
                    break;
                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;
                default:
                    showLog("setType default: " + type);
                    break;
            }
        }
    }

    // calculate dimension
    private void calculateDimension() {
        this.setCellSize(getWidth() / (COL + 1));
    }

    // set cell size
    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    // get cell size
    private float getCellSize() {
        return cellSize;
    }

    // update map information on auto mode
    public void updateMapInformation() throws JSONException {
        // current map information
        JSONObject mapInformation = this.getReceivedJsonObject();
        showLog("updateMapInformation --- mapInformation: " + mapInformation);

        JSONArray mapInfoJsonArray;
        JSONObject mapInfoJsonObject;
        String exploredString;


        if (mapInformation == null || mapInformation.names() == null  )
            return;

        for (int i = 0; i < mapInformation.names().length(); i++) {

            switch (mapInformation.names().getString(i)) {

                // set map layout and robot position
                case "map":
                    mapInfoJsonArray = mapInformation.getJSONArray("map");
                    mapInfoJsonObject = mapInfoJsonArray.getJSONObject(0);

                    // Render new position of robot
                    try {
                        if (canDrawRobot)
                            this.setOldRobotCoord(curCoord[0], curCoord[1]);
                        String direction = mapInfoJsonObject.getString("robotDirection");
                        switch (direction){
                            case "0":
                                direction = "up";
                                break;
                            case "1":
                                direction = "down";
                                break;
                            case "2":
                                direction = "right";
                                break;
                            case "3":
                                direction = "left";
                                break;
                            case "4":
                                direction = "up";
                                break;
                            case "5":
                                direction = "down";
                                break;
                            case "6":
                                direction = "up";
                                break;
                            case "7":
                                direction = "down";
                                break;
                        }

                        showLog("x: " + mapInfoJsonObject.getInt("robotX") + " y: " + mapInfoJsonObject.getInt("robotY"));

                        this.setCurCoord(mapInfoJsonObject.getInt("robotX") + 1, mapInfoJsonObject.getInt("robotY") + 1, direction);
                        canDrawRobot = true;
                    } catch (JSONException e){
                        e.printStackTrace();
                    }

                    // Render current map layout
                    exploredString = mapInfoJsonObject.getString("explored");
                    exploredString = "F" + exploredString; //Add F to start of string to prevent 0s at the start from being removed
                    exploredString = new BigInteger(exploredString, 16).toString(2);
                    exploredString = exploredString.substring(4); // Remove F that was added earlier

                    showLog("updateMapInformation.exploredString: " + exploredString + ", length: " + exploredString.length());

                    // set cells to explored, unexplored and obstacle
                    int x, y;
                    for (int j = 0; j < exploredString.length(); j+=2) {
                        // android coordinate
                        y = 19 - (j / 30);
                        x = 1 + (j/2) - ((19 - y) * 15);

                        if (!cells[x][y].type.equals("robot")){

                            // 11 = Virtual Wall
                            if ((String.valueOf(exploredString.charAt(j))).equals("1") && (String.valueOf(exploredString.charAt(j + 1))).equals("1") ) {

                                cells[x][y].setType("explored");

                                // 10 = Explored
                            } else if ((String.valueOf(exploredString.charAt(j))).equals("1") ){

                                cells[x][y].setType("explored");

                                // 01 = Obstacle
                            } else if ((String.valueOf(exploredString.charAt(j+1))).equals("1")){

                                this.setObstacleCoord(x, 20-y);

                                // 00 = Unexplored
                            } else {

                                cells[x][y].setType("unexplored");
                            }

                        }

                    }

                    break;

                // Custom image coord/type instruction
                case "image":
                    mapInfoJsonArray = mapInformation.getJSONArray("image");
                    showLog("updateMapInformation --- setImages: " + mapInfoJsonArray);
                    for (int j = 0; j < mapInfoJsonArray.length(); j++) {
                        mapInfoJsonObject = mapInfoJsonArray.getJSONObject(j);
                        showLog("updateMapInformation --- setImages: " + mapInfoJsonObject);
                            String imageString = mapInfoJsonObject.getString("imageString");

                            if (imageString.length() != 0){
                                while(imageString.length() > 0) {
                                    String nextChunk = imageString.substring(0,5);
                                    showLog("nextChunk: " + nextChunk);
                                    String imageX = nextChunk.substring(0,2);
                                    String imageY = nextChunk.substring(2,4);
                                    String imageType = nextChunk.substring(4);

                                    this.setImageCoordinate(Integer.parseInt(imageX), Integer.parseInt(imageY), imageType);

                                    imageString = imageString.substring(5);
                                }
                            }


                    }
                    break;

                default:
                    break;
            }

        }

        showLog("Exiting updateMapInformation");
        this.invalidate();
    }

    // set map information
    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        showLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        // to prevent screen from refreshing with old values
        backupMapInformation = receivedJsonObject;
    }

    // get received map information
    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    // get current map information
    public JSONObject getMapInformation() {
        showLog("getCreateJsonObject() :" + getCreateJsonObject());
        return this.getCreateJsonObject();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showLog("Entering onTouchEvent");
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize)); // convert to screen coordinate

            ToggleButton setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);
            ToggleButton setWaypointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);

            // if start coordinate status is true
            if (startCoordStatus) {
                // remove old starting coordinates
                if (canDrawRobot) {
                    // convert to screen coordinates
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        startCoord[1] = this.convertRow(startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0] + 1; x++)
                            for (int y = startCoord[1] - 1; y <= startCoord[1] + 1; y++)
                                cells[x][y].setType("unexplored");
                    }
                } else
                    canDrawRobot = true;
                // set new starting coordinates
                this.setStartCoord(column, row);
                // set start coordinate status to false
                startCoordStatus = false;

                String sp_x="" + (column-1);
                String sp_y ="" + (row-1);
                if ((column-1)<10){
                     sp_x = "0" + (column-1);
                }


                if ((row-1)<10)
                     sp_y = "0" + (row-1);

                MainActivity.setSPWP("0", sp_x, sp_y);

                // update the axis on the screen
                updateRobotAxis(column-1, row-1, "up");
                // if the button is checked, uncheck it
                if (setStartPointToggleBtn.isChecked())
                    setStartPointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            // if waypoint coordinate status is true
            if (setWaypointStatus) {
                int[] waypointCoord = this.getWaypointCoord();
                // if waypoint coordinate is valid
                if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
                    cells[waypointCoord[0]][this.convertRow(waypointCoord[1])].setType("unexplored");
                // set start coordinate status to false
                setWaypointStatus = false;
                // print out the message sent to other device
                try {
                    this.setWaypointCoord(column, row);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // if the button is checked, uncheck it
                if (setWaypointToggleBtn.isChecked())
                    setWaypointToggleBtn.toggle();
                this.invalidate();
                return true;
            }
            // if obstacle status is true
            if (setObstacleStatus) {
                this.setObstacleCoord(column, row);
                this.invalidate();
                return true;
            }
            // if explored status is true
            if (setExploredStatus) {
                cells[column][20 - row].setType("explored");
                this.invalidate();
                return true;
            }
            // if unset cell status is true
            if (unSetCellStatus) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20 - row].setType("unexplored");
                for (int i = 0; i < obstacleCoord.size(); i++)
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row)
                        obstacleCoord.remove(i);
                this.invalidate();
                return true;
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    // toggle all button if enabled/checked, except for the clicked button
    public void toggleCheckedBtn(String buttonName) {
        ToggleButton setStartPointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setStartPointToggleBtn);
        ToggleButton setWaypointToggleBtn = ((Activity) this.getContext()).findViewById(R.id.setWaypointToggleBtn);
        Button obstacleImageBtn = ((Activity) this.getContext()).findViewById(R.id.obstacleImageBtn);
        Button exploredImageBtn = ((Activity) this.getContext()).findViewById(R.id.exploredImageBtn);
        Button clearImageBtn = ((Activity) this.getContext()).findViewById(R.id.unexploredImageBtn);

        if (!buttonName.equals("setStartPointToggleBtn"))
            if (setStartPointToggleBtn.isChecked()) {
                this.setStartCoordStatus(false);
                setStartPointToggleBtn.toggle();
            }
        if (!buttonName.equals("setWaypointToggleBtn"))
            if (setWaypointToggleBtn.isChecked()) {
                this.setWaypointStatus(false);
                setWaypointToggleBtn.toggle();
            }
        if (!buttonName.equals("exploredImageBtn"))
            if (exploredImageBtn.isEnabled())
                this.setExploredStatus(false);
        if (!buttonName.equals("obstacleImageBtn"))
            if (obstacleImageBtn.isEnabled())
                this.setSetObstacleStatus(false);
        if (!buttonName.equals("clearImageBtn"))
            if (clearImageBtn.isEnabled())
                this.setUnSetCellStatus(false);
    }

    // create jsonobject for map information
    public JSONObject getCreateJsonObject() {
        showLog("Entering getCreateJsonObject");
        String exploredString = "11";
        String obstacleString = "";
        String hexStringObstacle = "";
        String hexStringExplored = "";
        BigInteger hexBigIntegerObstacle, hexBigIntegerExplored;
        int[] waypointCoord = this.getWaypointCoord();
        int[] curCoord = this.getCurCoord();
        String robotDirection = this.getRobotDirection();
        List<int[]> obstacleCoord = new ArrayList<>(this.getObstacleCoord());
        List<String[]> imageCoord = new ArrayList<>(this.getImageCoord());

        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);

        // JSONObject to contain individual JSONArray which contains another JSONObject
        // passing of map information
        JSONObject map = new JSONObject();
        for (int y = ROW - 1; y >= 0; y--)
            for (int x = 1; x <= COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot") || cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image"))
                    exploredString = exploredString + "1";
                else
                    exploredString = exploredString + "0";
        exploredString = exploredString + "11";
        showLog("exploredString: " + exploredString);

        hexBigIntegerExplored = new BigInteger(exploredString, 2);
        showLog("hexBigIntegerExplored: " + hexBigIntegerExplored);
        hexStringExplored = hexBigIntegerExplored.toString(16);
        showLog("hexStringExplored: " + hexStringExplored);

        for (int y = ROW - 1; y >= 0; y--)
            for (int x = 1; x <= COL; x++)
                if (cells[x][y].type.equals("explored") || cells[x][y].type.equals("robot"))
                    obstacleString = obstacleString + "0";
                else if (cells[x][y].type.equals("obstacle") || cells[x][y].type.equals("image"))
                    obstacleString = obstacleString + "1";
        showLog("Before loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        while ((obstacleString.length() % 8) != 0) {
            obstacleString = obstacleString + "0";
        }

        showLog("After loop: obstacleString: " + obstacleString + ", length: " + obstacleString.length());

        if (!obstacleString.equals("")) {
            hexBigIntegerObstacle = new BigInteger(obstacleString, 2);
            showLog("hexBigIntegerObstacle: " + hexBigIntegerObstacle);
            hexStringObstacle = hexBigIntegerObstacle.toString(16);
            if (hexStringObstacle.length() % 2 != 0)
                hexStringObstacle = "0" + hexStringObstacle;
            showLog("hexStringObstacle: " + hexStringObstacle);
        }
        try {
            map.put("explored", hexStringExplored);
            map.put("length", obstacleString.length());
            if (!obstacleString.equals(""))
                map.put("obstacle", hexStringObstacle);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        JSONArray jsonMap = new JSONArray();
        jsonMap.put(map);

        // passing of waypoint coordinates
        JSONArray jsonRobot = new JSONArray();
        if (curCoord[0] >= 2 && curCoord[1] >= 2)
            try {
                JSONObject robot = new JSONObject();
                robot.put("x", curCoord[0]);
                robot.put("y", curCoord[1]);
                robot.put("direction", robotDirection);
                jsonRobot.put(robot);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // passing of waypoint coordinates
        JSONArray jsonWaypoint = new JSONArray();
        if (waypointCoord[0] >= 1 && waypointCoord[1] >= 1)
            try {
                JSONObject waypoint = new JSONObject();
                waypoint.put("x", waypointCoord[0]);
                waypoint.put("y", waypointCoord[1]);
                setWaypointStatus = true;
                jsonWaypoint.put(waypoint);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // passing of obstacle coordinates
        JSONArray jsonObstacle = new JSONArray();
        for (int i = 0; i < obstacleCoord.size(); i++)
            try {
                JSONObject obstacle = new JSONObject();
                obstacle.put("x", obstacleCoord.get(i)[0]);
                obstacle.put("y", obstacleCoord.get(i)[1]);
                jsonObstacle.put(obstacle);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // passing of image coordinates
        JSONArray jsonImage = new JSONArray();
        for (int i = 0; i < imageCoord.size(); i++) {
            try {
                JSONObject image = new JSONObject();
                image.put("imageX", Integer.parseInt(imageCoord.get(i)[0]));
                image.put("imageY", Integer.parseInt(imageCoord.get(i)[1]));
                image.put("imageType", imageCoord.get(i)[2]);
                jsonImage.put(image);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // passing of image coordinates
        JSONArray jsonStatus = new JSONArray();
        try {
            JSONObject status = new JSONObject();
            status.put("status", robotStatusTextView.getText().toString());
            jsonStatus.put(status);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // JSONObject to contain all the JSONArray
        mapInformation = new JSONObject();
        try {
            mapInformation.put("map", jsonMap);
            mapInformation.put("robot", jsonRobot);
            if (setWaypointStatus) {
                mapInformation.put("waypoint", jsonWaypoint);
                setWaypointStatus = false;
            }
            mapInformation.put("obstacle", jsonObstacle);
            mapInformation.put("image", jsonImage);
            mapInformation.put("status", jsonStatus);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        showLog("Exiting getCreateJsonObject");
        return mapInformation;
    }

    // reset map
    public void resetMap() {
        showLog("Entering resetMap");
        // reset screen text
        TextView robotStatusTextView = ((Activity) this.getContext()).findViewById(R.id.robotStatusTextView);
        ToggleButton manualAutoToggleBtn = ((Activity) this.getContext()).findViewById(R.id.manualAutoToggleBtn);
        Switch phoneTiltSwitch = ((Activity) this.getContext()).findViewById(R.id.phoneTiltSwitch);
        Button manualUpdateBtn = ((Activity) this.getContext()).findViewById(R.id.manualUpdateBtn);
        manualUpdateBtn.setEnabled(true);
        updateRobotAxis(0, 0, "None");
        robotStatusTextView.setText("None");
        sharedPreferences();
        editor.putString("receivedText", "");
        editor.putString("sentText", "");
        editor.putString("image", "");
        editor.commit();

        if (manualAutoToggleBtn.isChecked())
            manualAutoToggleBtn.toggle();
        this.toggleCheckedBtn("None");

        if (phoneTiltSwitch.isChecked()) {
            phoneTiltSwitch.toggle();
            phoneTiltSwitch.setText("TILT OFF");
        }

        // reset all the values
        receivedJsonObject = null;      //new JSONObject();
        backupMapInformation = null;    //new JSONObject();
        startCoord = new int[]{-1, -1};         // 0: col, 1: row
        curCoord = new int[]{-1, -1};           // 0: col, 1: row
        oldCoord = new int[]{-1, -1};           // 0: col, 1: row
        robotDirection = "None";        // reset the robot direction
        autoUpdate = false;             // reset it to manual mode
        imageCoord = new ArrayList<>(); // reset the image coordinates array list
        obstacleCoord = new ArrayList<>();  // reset the obstacles coordinate array list
        waypointCoord = new int[]{-1, -1};      // 0: col, 1: row
        mapDrawn = false;           // set map drawn to false
        canDrawRobot = false;       // set can draw robot to false
        validPosition = false;      // set valid position to false
       // Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_error);   // default image for bitmap

        showLog("Exiting resetMap");
        this.invalidate();
    }

    // for activating sharedPreferences
    private void sharedPreferences() {
        // set TAG and Mode for shared preferences
        sharedPreferences = this.getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // show log message
    private void showLog(String message) {
        Log.d(TAG, message);
    }
}