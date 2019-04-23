/**
 * 房间和迷宫的连接
 * 
 * 每个房间都需要能走通
 * 
 * 0.迷宫不保证连续
 *  1.
 * 1.房间可能被房间包围导致无法连接迷宫
 * 2.所有房间连接到迷宫或已经连接到迷宫的房间
 * 
 * 1.寻找一个房间作为起点，房间里的所有地块加入已经连接表，开门
 * 2.
 * 3.
 * 
 * 迷宫不可能紧靠，否则生成时就会生成过去。如果迷宫分为多个部分则必然是被房间分隔，因此所有可以开门的位置只有房间与房间和房间到迷宫。也就是说所有开门位置都可以以房间为起点进行搜索
 * 生成时在连接房间和迷宫时没有连接到的位置相当于不可见，这使得必须要有某种提示信息存在，否则只能进行高成本的遍历
 * 可以在房间外围制造连接点，但问题是有的房间距离迷宫和其他房间有厚度为2的墙，这使得连接点具有方向
 * 更大的问题是两个房间之间的连接点也有方向性，此时连接点如何表现出两个方向性？
 * 或者连接点没有方向性而是在打通的时候有方向性？
 * 
 * 连接点是由房间的四条边向外直线延伸，直到找到空地。因此和连接点相邻的地块必然在房间的正上下或正左右方向，不会有斜着的情况。
 * 
 * 
 * 连接连接前很可能需要把所有房间、迷宫都保存起来，之后要有一个合并方法，把连接起来的房间和迷宫合并到一个叫“主区域”的表里。
 * 
 * a.判断一个连接点是不是可以移除的方法应该是（一个连接点上下左右四个方向，长度最大为2，除了连接点外最近的地块，是不是墙或主区域。如果不是墙也不是主区域，则说明这个连接点能连接到没连接到的位置。）
 * 
 * b.移除连接点的第二种思路：以房间为单位移除连接点。因为迷宫之间不可能有连接点，那么所有连接点肯定都能从房间为起点找到（可以直接设计为连接点从房间向外生成）
z*   既然连接点都可以通过房间找到，那就方便多了：
z*   在连接一个房间之后，遍历这个房间的所有连接点，从房间内向外走，看最近的空地是不是主区域的空地，是的话就说明走过的所有连接点都没用了，不是的话说明连接点指向没有连接的区域
 *   
 *   
z* 连接点部分：
 * 
z*  遍历房间
 *  {
z*      遍历房间上边
 *      {
z*          遍历每个地块
 *          {
z*              递归或循环向上查找地块 (找3格)
 *              {
 *                  if (到达了地图边缘)
 *                      return
 *              
 *                  if (找到一个是空地的地块) 【此处有扩展性问题】假设有个房间是内凹的，房间将可能自己和自己创建连接点，可以通过判断是不是房间自己的方块来解决
 *                  {
z*                      一路上所有墙创建连接点
 *                      return
 *                  }
 *              }
 *          }
 *      }
 *      
z*      遍历其他三边，类似于左边的处理
 *  }
 *  
 *  
z*  连接部分：
 *  
z*  随机选一个房间，所有地块加入主区域表
 *  
 *  while(还有连接点)
 *  {
 *      List 相邻的连接点 = 遍历主区域地块，获取所有和主区域相邻的连接点
 *      
z*      从相邻的连接点里随机一个连接点
z*      在这个连接点上下左右四个方向里找到主区域的地块 -> 这个地块到连接点的方向就是相邻区域的方向
z*      沿着这个方向找到下一个空地
z*      把中间这段墙打通
z*      把下一个区域所有方块加入到主区域
 *      
 *      if(主区域方向的地块属于某个房间)
z*          移除房间连接点(主区域房间)
 *      if(相邻区域的地块属于某个房间)
z*          移除房间连接点(相邻区域房间)
 *  }
 *  
 *  void 移除房间连接点(Room 房间)
 *  {
z*      遍历房间上边
 *      {
 *          if(地块的上边一格是连接点)
 *          {
z*              向上找到下一个空地
 *              if(这个空地在主区域里)
 *              {
z*                  移除经过的所有格子里的连接点
 *              }
 *          }
 *      }
 *      
z*      遍历其他三个边
 *  }
 */

package spown.spowner;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import map.Map;
import map.quad.Quad;
import map.quad.QuadType;
import random.Random;
import spown.zones.MazeZone;
import spown.zones.RoomZone;
import spown.zones.Zone;
import vector.Vector;

public class MapConnector {
    private Map _map;
    private ArrayList<RoomZone> _rooms;
    private ArrayList<Zone> _zones;
    private MapSpownData _spownData;
    private HashSet<Quad> _mainZoneQuads; // 利用 HashSet 的无序性来进行随机查找与主区域相邻的连接点
    private boolean[][] _connectPoints;

    /**
     * 连接房间和迷宫
     * 
     * @param map
     * @param rooms
     */
    public Map ConnectRoomsAndMaze(Map map, ArrayList<RoomZone> rooms, ArrayList<MazeZone> mazes,
            MapSpownData spownData) {
        try {
            setupConnector(map, rooms, mazes, spownData);
            spownConnectPoints();
            StartConnect();
            return _map;
        } finally {
            clearConnector();
        }
    }

    /**debug记录
    private void printConnectPoints() {
        StringBuilder string = new StringBuilder();
    
        for (int y = 0; y < _map.height; y++) {
            for (int x = 0; x < _map.width; x++) {
                string.append(getQuadString(_map.getQuad(x, y)));
            }
            string.append("\n");
        }
    
        System.out.println("↓生成点↓");
        System.out.println(string.toString());
        System.out.println("↑生成点↑");
    }
    
    private String getQuadString(Quad quad) {
        //生成点应该只在墙上生成
        //主区域应该全部是空地
    
        if (isConnectPoint(quad))
            if (quad.getType() == QuadType.WALL)
                return "生"; // 生成点、是墙，说明正确，用“生”表示
            else
                return "聲"; // 生成点，不是墙，说明生成点生成错误
    
        if (quad.getType() == QuadType.WALL)
            if (!_mainZoneQuads.contains(quad))
                return "墙";
            else
                return "繦"; // 墙，是主区域，说明加入主区域步骤错误
    
        if (quad.getType() == QuadType.FLOOR)
            if (_mainZoneQuads.contains(quad))
                return "十";
            else
                return "一";
    
        return "错";
    }
    
    //测试结果显示极有可能是判断是否移除连接点的方法出错了
    
    //1.房间连迷宫、墙为纵向、厚度为2、左主区域右非主区域：：靠近主区域的（左侧的）厚度1的墙被移除了连接点
    //2.房间连迷宫、墙为横向、厚度为1、上下都是主区域、检测区（迷宫）在上方：：墙的连接点没有被移除
    //3.房间连迷宫、墙为横向、厚度为2、上下都是主区域、监测区（迷宫）在下方：：靠近选择区（靠下）的生成点被移除，远离（靠上）的仍然存在
    //4.房间连迷宫、墙为纵向、厚度为2、左右都是主区域、检测区（迷宫）在右侧：：墙的连接点没有移除
    //5.房间连迷宫、墙为纵向、厚度为1、左右都是主区域、检测区（迷宫）在左侧：：墙的连接点没有移除。但同时，其他情况相同的墙的连接点被移除【没有获取到全部的连接点？】
    //6.【疑似】房间与迷宫相连、墙为横向，厚度为1、上下都是主区域，检测区无法确认：：之前没有正确清理的连接点被正确清理了
    //7.房间连迷宫、墙为纵向、厚度为1、上下都是主区域、检测区（迷宫）在左侧：：墙的连接点没有移除
    //8.房间连迷宫、墙为横向、厚度为2、下主区域上非主区域：：靠近主区域的（下方的）厚度1的墙被移除了连接点
    //9.房间连迷宫、墙为横向、厚度为2、上主区域下非主区域：：靠近主区域的（上方的）厚度1的墙被移除了连接点
    
    //房间连房间暂未发现错误
    
    //迷宫连房间暂未发现错误
    
    /*
     *  可能的错误位置：
    墙墙墙墙墙墙墙墙墙墙墙墙墙墙墙墙
    墙一生十十十十墙十十十十生一墙墙
    墙一生十十十十十十十十十生一墙墙
    墙一生十十十十生十十十十生一墙墙
    墙一生十十十十生十十十十生一墙墙
    墙一墙生墙生墙墙十十十十生一墙墙
    墙一墙生墙一墙墙生生墙生墙一墙墙
    墙一一一墙生墙墙生生墙一一一墙墙
    墙一墙墙墙一一一一一生一墙一墙墙
    墙一一一生一一一一一生一墙一墙墙
    墙一墙一生一一一一一墙墙墙一墙墙
    墙一墙一生一一一一一生一墙一墙墙
    墙一墙墙墙一一一一一生一墙一墙墙
    墙一一一墙生生生生生墙一一一墙墙
    墙墙墙墙墙一一一一一墙墙墙墙墙墙
    墙墙墙墙墙墙墙墙墙墙墙墙墙墙墙墙
     *  在上中部，两个生成点的三个方向都是主区域，此时如果上方的生成点向下移除生成点，将会导致走出三步也找不到主区域，达不到清除条件，生成点无法正常清除
     *  房间清除生成点是从四面向外清理，因此不会发生这个错误，但迷宫清除生成点是随机选择方向，这就导致了清理错误
     *  解决方式是在清除迷宫生成点时遍历所有主区域，并从主区域向所有方向发出清理
     */

    private void setupConnector(Map map, ArrayList<RoomZone> rooms, ArrayList<MazeZone> mazes, MapSpownData spownData) {
        _map = map;
        _rooms = rooms;
        setupZones(rooms, mazes);
        _spownData = spownData;
        _mainZoneQuads = new HashSet<Quad>();
        setupConnects(map);
    }
    
    private void setupZones(ArrayList<RoomZone> rooms,ArrayList<MazeZone> mazes) {
        _zones = new ArrayList<Zone>();
        _zones.addAll(rooms);
        _zones.addAll(mazes);
//        System.out.println("总区域数 = " + _zones.size());
    }
    
    private void setupConnects(Map map) {
        _connectPoints = new boolean[map.width][map.height];

        for (int y = 0; y < map.height; y++)
            for (int x = 0; x < map.width; x++)
                removeConnectPoint(x, y);
    }

    private void clearConnector() {
        _map = null;
        _rooms = null;
        _zones = null;
        _spownData = null;
        _mainZoneQuads = null;
        _connectPoints = null;
    }

    /**
     * 生成连接点
     */
    private void spownConnectPoints() {
        /*
         *  遍历房间
         *  {
         *      生成这个房间的连接点
         *  }
         */
        //System.out.println("开始生成连接点");
        for (RoomZone room : _rooms)
            spownARoomConnectPoints(room);
    }

    /**
     * 生成一个房间的连接点
     * 
     * @param room
     */
    private void spownARoomConnectPoints(RoomZone room) {
        /**
         *  遍历房间上边每个地块
         *  {
         *      生成这个地块的向上的连接点
         *  }
         *  其他三边类似
         */
        //System.out.println("生成房间 " + room + " 的连接点");
        for (Quad quad : room.getTopQuads())
            spownAQuadConnectPoints(quad, Vector.UP);
        for (Quad quad : room.getRightQuads())
            spownAQuadConnectPoints(quad, Vector.RIGHT);
        for (Quad quad : room.getBottomQuads())
            spownAQuadConnectPoints(quad, Vector.DOWN);
        for (Quad quad : room.getLeftQuads())
            spownAQuadConnectPoints(quad, Vector.LEFT);
    }

    /**
     * 生成一个地块相邻的连接点
     * 
     * @param startPosition
     * @param direction
     */
    private void spownAQuadConnectPoints(Point startPosition, Vector direction) {
        //System.out.println("在 " + quadPosition + " 位置向 " + direction + " 方向生成连接点");

        spownAQuadConnectPoints(startPosition, direction, 1); // 传 1 是因为 0 是房间边缘的地块，是空地，会直接结束生成

        //printConnectPointsNumber();
    }

    /** 用于测试连接点生成的测试输出
    private void printConnectPointsNumber() {
        int num = 0;
        for (int y = 0; y < _connectPoints[0].length; y++)
            for (int x = 0; x < _connectPoints.length; x++)
                if (_connectPoints[x][y])
                    num++;
        System.out.println("连接点数量 = " + num);
    }
    */

    private boolean spownAQuadConnectPoints(Point startPosition, Vector direction, int step) {
        /*
         *  可以创建连接点为 true，不能创建连接点为 false
         * 
         *  if(步数 > 3) 第三步还没找到空地，说明此路不通
         *      return false
         *  
         *  Point 当前点 = 根据起点、方向、步数计算当前点
         *  if(超出地图)
         *  {
         *      return false
         *  }
         *  
         *  if(当前点的地块是空地)
         *  {
         *      return true
         *  }
         *  
         *  if(递归下一步 true)
         *  {
         *      在当前点创建连接点
         *      return true
         *  }
         *  
         *  return false
         */
        if (step > 3) // 根据算法，房间到最近的房间或迷宫的距离不会超过两个墙，那么当长度延伸到3的时候，要么已经遇到了空地，要么就没有走到正确的路线上
            return false;

        Point currentPoint = new Point(startPosition.x + direction.x * step, startPosition.y + direction.y * step);
        if (!_map.contains(currentPoint))
            return false;

        if (_map.getType(currentPoint) == QuadType.FLOOR)
            return true;

        if (spownAQuadConnectPoints(startPosition, direction, step + 1)) /*如果下一步能遇到空地*/ {
            addConnectPoint(currentPoint); // 则在这一个位置就应该是墙了，加入连接点
            return true;
        }

        return false;
    }

    /**
     * 添加连接点
     * 
     * @param point
     */
    private void addConnectPoint(Point point) {
        addConnectPoint(point.x, point.y);
    }

    /**
     * 添加连接点
     * 
     * @param x
     * @param y
     */
    private void addConnectPoint(int x, int y) {
        _connectPoints[x][y] = true;
    }

    /**
     * 移除连接点
     * 
     * @param point
     */
    private void removeConnectPoint(Point point) {
        removeConnectPoint(point.x, point.y);
    }

    /**
     * 移除连接点
     * 
     * @param x
     * @param y
     */
    private void removeConnectPoint(int x, int y) {
        _connectPoints[x][y] = false;
    }

    /**
     * 判断一个位置是不是连接点
     * 
     * @param point
     * @return
     */
    private boolean isConnectPoint(Point point) {
        return isConnectPoint(point.x, point.y);
    }

    /**
     * 判断一个位置是不是连接点
     * 
     * @param x
     * @param y
     * @return
     */
    private boolean isConnectPoint(int x, int y) {
        return _connectPoints[x][y];
    }

    /**
     * 开始连接
     */
    private void StartConnect() {
        /*
         *  随机选一个房间，所有地块加入主区域表
         *  
         *  Point 连接点
         *  while((随机获取一个主区域相邻的连接点) != null)
         *  {
         *      连接连接点
         *  }
         */
        connectStartRoom(); // 随机选一个房间，所有地块加入主区域表

        Point connectPoint = null;
        while ((connectPoint = getRandomConnectPoint()) != null)
            connectAConnectPoint(connectPoint);
    }

    /**
     * 连接第一个房间
     */
    private void connectStartRoom() {
        /*
         *  Room 房间 =  随机出一个房间
         *  房间中的地块加入到主区域
         *  
         *  还需要其他操作吗？似乎不需要，似乎没有清理连接点的必要性
         */
        RoomZone room = getRandomRoom();
        addARoomQuadsToMainZone(room);
        //System.out.println("第一个房间 = " + room);
    }

    private RoomZone getRandomRoom() {
        return _rooms.get(Random.Range(0, _rooms.size()));
    }

    /**
     * 将一个房间的地块加入到主区域
     * 
     * @param room
     */
    private void addARoomQuadsToMainZone(final RoomZone room) {
        _mainZoneQuads.addAll(Arrays.asList(room.getQuads()));
    }

    /**
     * 获取随机与主区域相连的连接点
     * 
     * @return
     */
    private Point getRandomConnectPoint() {
        /*
         *  遍历所有主区域地块（利用 HashSet 的无序性实现随机）
         *  {
         *      获取当前地块相邻的连接点
         *      if(连接点 != null)
         *      {
         *          return 连接点
         *      }
         *  }
         *  
         *  遍历完了也没找到，return null
         */
        Point connectPoint = null;
        for (Quad quad : _mainZoneQuads) {
            connectPoint = getContiguousConnectPoint(quad);
            if (connectPoint != null)
                return connectPoint;
        }
        return null;
    }

    /**
     * 获取一个地块上下左右四个相邻位置中第一个是连接点的那个，如果四个都不是连接点则返回 null
     * 
     * @param quad
     * @return
     */
    private Point getContiguousConnectPoint(final Quad quad) {
        /*
         *  遍历当前地块的上下左右四个Point
         *  {
         *      if (找到连接点了)
         *          return 找到的连接点
         *  }
         *  
         *  没找到 return null
         */
        if (isConnectPoint(quad.x, quad.y + 1))
            return new Point(quad.x, quad.y + 1);
        if (isConnectPoint(quad.x + 1, quad.y))
            return new Point(quad.x + 1, quad.y);
        if (isConnectPoint(quad.x, quad.y - 1))
            return new Point(quad.x, quad.y - 1);
        if (isConnectPoint(quad.x - 1, quad.y))
            return new Point(quad.x - 1, quad.y);
        return null;
    }

    /**
     * 从一个连接点出发，连接连接点连接到的区域
     * 
     * @param connectPoint
     */
    private void connectAConnectPoint(final Point connectPoint) {
        /*
         *  Point 起点 = 获取相邻的主区域点(连接点)
         *  Vector 方向 = 连接点 - 主区域点
         *  
         *  for(step = 1;;step++)
         *  {
         *      Point 当前点 = 根据起点、方向、步数计算当前点
         *      
         *      if(当前点不是空地)
         *      {
         *          连接这个地块（应该是墙）
         *      }
         *      else
         *      {
         *          根据地块获取区域()
         *          连接这个区域()
         *          return
         *      }
         *  }
         */
        Point startPoint = getContiguousMainQuad(connectPoint);
        Vector direction = new Vector(connectPoint.x - startPoint.x, connectPoint.y - startPoint.y);

        for (int step = 1;; step++) {
            Quad currentQuad = _map.getQuad(startPoint.x + direction.x * step, startPoint.y + direction.y * step);

            if (_map.getType(currentQuad) != QuadType.FLOOR) {
                connectAQuad(currentQuad);
            } else {
                Zone zone = getZoneByQuad(currentQuad);
                connectAZone(zone);
                return;
            }
        }
    }

    /**
     * 获取地块所属的区域
     * 
     * @param quad
     * @return
     */
    private Zone getZoneByQuad(Quad quad) {
        /*
         *  遍历区域
         *      if (区域包含这个地块)
         *          return 这个区域
         *  return null
         */
        for (Zone zone : _zones)
            if (zone.contains(quad))
                return zone;
        return null;
    }

    /**
     * 连接一个区域
     * 
     * @param zone
     */
    private void connectAZone(Zone zone) {
        /*
         *  将这个区域所有地块加入主区域
         *  清理这个区域的连接点
         */
        addZoneQuadsToMainZone(zone);
        clearAZoneConnectPoints(zone);
    }

    /**
     * 将一个区域的所有地块加入主区域
     * 
     * @param zone
     */
    private void addZoneQuadsToMainZone(Zone zone) {
        _mainZoneQuads.addAll(Arrays.asList(zone.getQuads()));
    }

    /**
     * 清理一个区域的连接点
     * 
     * @param zone
     */
    private void clearAZoneConnectPoints(Zone zone) {
        /*
         *  遍历这个区域所有的地块
         *  {
         *      清理这个地块的连接点
         *  }
         */
        for (Quad quad : zone.getQuads())
            clearAQuadConnectPoint(quad);
    }

    /**
     * 获取一个地块相邻的一个主区域地块
     * 
     * @param center
     * @return
     */
    private Point getContiguousMainQuad(Point center) {
        if (_mainZoneQuads.contains(_map.getQuad(center.x, center.y + 1)))
            return _map.getQuad(center.x, center.y + 1);
        if (_mainZoneQuads.contains(_map.getQuad(center.x + 1, center.y)))
            return _map.getQuad(center.x + 1, center.y);
        if (_mainZoneQuads.contains(_map.getQuad(center.x, center.y - 1)))
            return _map.getQuad(center.x, center.y - 1);
        if (_mainZoneQuads.contains(_map.getQuad(center.x - 1, center.y)))
            return _map.getQuad(center.x - 1, center.y);
        return null;
    }

    /**
     * 把一个地块连接到主区域
     * 
     * @param quadPoint
     */
    private void connectAQuad(Point quadPoint) {
        /*
         *  打穿墙
         *  加进主区域
         *  清除连接点
         */
        _map.setType(quadPoint, QuadType.FLOOR);
        _mainZoneQuads.add(_map.getQuad(quadPoint));
        removeConnectPoint(quadPoint);
    }


    /**
     * 清除一个地块的连接点
     * 
     * @param startPoint
     * @param direction 这个地块的连接点相对于地块的方向
     */
    private void clearAQuadConnectPoint(Point startPoint, Vector direction) {
        clearConnectPoint(startPoint, direction, 1); // 从第一步开始，第零步是房间边缘的地块，肯定不是连接点
        //printConnectPoints();
    }

    private boolean clearConnectPoint(final Point startPoint, final Vector direction, final int step) {
        /*
         *  设计上如果方向正确连接点最少没有最多两个
         *      0个：此路不通，应直接结束
         *      1个：前进第二步后会到达不是墙的地块
         *      2个：前进第三步后会到达不是墙的地块
         *  得出结论：
         *      1.最多走三步
         *      2.遇到不是连接点的位置应该停止
         *      3.停止的地方如果是墙，则说明此路不通，要么是连接点生成错了要么是找错了，为了安全不做清理
         *      4.停止的地方如果是空地，并且这个空地属于主区域。说明这串连接点已经没用了，可以移除
         */
        /*
         *  以 true 作为可以移除，以 false 作为不可移除
         * 
         *  if (到达的位置不是连接点) -> 2.不是连接点的位置停止
         *  {
         *      if (到达的位置是墙)
         *          此路不通 return false -> 3.停止的地方是墙，此路不通
         *          
         *      if (到达的位置是空地 && 这个空地是主区域的空地)
         *          走对了 return true -> 4.停止的地方是空地，可以走通
         *  }
         *  
         *  if (向下递归是 true)
         *  {
         *      移除自己这一格的连接点
         *      return true
         *  }
         *  else
         *  {
         *      return false
         *  }
         * 
         *  if (步数 > 3) -> 1.最多走三步
         *      逻辑上发生严重错误 return false
         *  
         *  return false -> 默认是 false
         */
        if (step > 3) {
//            System.out.println("因超出三步错误");
            return false;
        }

        Point currentPoint = new Point(startPoint.x + direction.x * step, startPoint.y + direction.y * step);

        if (!_map.contains(currentPoint)) {
//            System.out.println("搜索连接点超出地图边界");
            return false;
        }

        if (!isConnectPoint(currentPoint)) {
            if (_mainZoneQuads.contains(_map.getQuad(currentPoint.x, currentPoint.y)))
                return true;
            else {
//                System.out.println("因不是连接点返回");
                return false;
            }
        }

        if (clearConnectPoint(startPoint, direction, step + 1)) {
            removeConnectPoint(currentPoint);
            return true;
        }

//        System.out.println("在最后判断错误");
        return false;
    }

    /**
     * 清除一个地块四周的生成点
     * 
     * @param quad
     */
    private void clearAQuadConnectPoint(Quad quad) {
        clearAQuadConnectPoint(quad, Vector.UP);
        clearAQuadConnectPoint(quad, Vector.RIGHT);
        clearAQuadConnectPoint(quad, Vector.DOWN);
        clearAQuadConnectPoint(quad, Vector.LEFT);
    }
}
