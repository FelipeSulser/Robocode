package hpsr;
import robocode.*;
import robocode.util.Utils;

import java.awt.Color;
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;


/**
 * <p>
 * Felipe's bot
 * Robot using Wavesurfer technique
 * </p>
 * Moves in a cool manner, sneaky af
 * More info about wavesurfers 
 * http://blog.bagesoft.com/m/post/464
 * http://robowiki.net/cgi-bin/robowiki?WaveSurfing/Tutorial
 * @author Felipe Sulser
 * 
 */
public class HangukBot extends AdvancedRobot
{
     private static double myEnergy;
     private static  Point2D.Double futurePos;
     private static Point2D.Double myPos;
     private BadBoy objetivo;
     private static  Point2D.Double prevPos;
     //No diamond inference?
     private  static Hashtable<String,BadBoy> enemies = new Hashtable<String,BadBoy>();
        
        
        public class BadBoy {
            public Point2D.Double pos;
            public double energy;
            public boolean hp;
    }
        
       
        
        public void run()
        {
            
                setColors(Color.LIGHT_GRAY, Color.BLACK, Color.WHITE); 
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY); //extra cheeky radar strat
                
             
                setAdjustRadarForGunTurn(true);
                setAdjustGunForRobotTurn(true);
               
                
                futurePos = prevPos = myPos = new Point2D.Double(getX(), getY());
                objetivo = new BadBoy();
                
               while(true){
                        //refresco variables de entorno
                        myPos = new Point2D.Double(getX(),getY());
                        myEnergy = getEnergy();
                        
                       //scan unos 7 ticks y muevo y disparo, todo a su tiempo
                      
                        if(objetivo.hp && getTime()>7) {
                                this.trackAndFire();
                        }
                        
                        
                        //Error corrected with this, now behaviour is nice,  gira bien!
                        execute();
                        scan();
               }
        }
        
    
        
        
     /**
      * General rule of calculating a point given origin, angle and distance
      * http://math.stackexchange.com/questions/143932/calculate-point-given-x-y-angle-and-distance
      * @param p
      * @param dist
      * @param ang
      * @return point, used to establish localizacion del enemy.
      */
    private static Point2D.Double hallarPunto(Point2D.Double p, double dist, double ang) {
            return new Point2D.Double(p.x + dist*Math.sin(ang), p.y + dist*Math.cos(ang));
    }
    
    //Angle between two points
    
    /**
     * http://stackoverflow.com/questions/7586063/how-to-calculate-the-angle-between-a-line-and-the-horizontal-axis
     * @param p2
     * @param p1
     * @return
     */
    private static double angulo(Point2D.Double p2,Point2D.Double p1){
        
            return Math.atan2(p2.x - p1.x, p2.y - p1.y);
    }
    
        
    
    
    //here comes the magic
    
    /**
     * Obj is being tracked by us, first we shoot if we can and got energy and then we move
     * where r we moving?, you may ask. Well, to the spot which eval function optimizes
     */
        public void trackAndFire() {
                double theta;
                double distanciaObjetivo = myPos.distance(objetivo.pos);
                //Hallar el proximo punto en un perímetro definido
                //(30,30) elimina bordes y despues el -60 para la longitud de los dados
                Rectangle2D.Double perimetro = new Rectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60);
                
      
                
                //if my cannon is locked and ready and i got some energy left fire with
                //appropiate energy to not get stuck, fire in the execute rather than now
                
                if(getGunTurnRemaining() == 0 && myEnergy > 1) {
                        setFire( Math.min(Math.min(myEnergy/6.0, 1000/distanciaObjetivo), objetivo.energy/3.0) );
                }
               
                //any other case, ill get aim lock with this function
                //normalize sets between pi -pi
                setTurnGunRightRadians(Utils.normalRelativeAngle(angulo(objetivo.pos, myPos) - getGunHeadingRadians()));
                

                double distNextPunto = myPos.distance(futurePos);
                
               
                if(distNextPunto > 20) {
                    //aun estamos lejos
                    
                    
                    //theta es el angulo que hemos de cortar para ponernos "encarando" bien
                    theta = angulo(futurePos, myPos) - getHeadingRadians();
                    
                    
                    double sentido = 1;
                    
                    if(Math.cos(theta) < 0) {
                            theta += Math.PI;
                            sentido = -1;
                    }
                    
                    setAhead(distNextPunto * sentido);
                    theta = Utils.normalRelativeAngle(theta);
                    setTurnRightRadians(theta);
                    
                    if(theta > 1)setMaxVelocity(0.0);
                    else setMaxVelocity(8.0); // max
                    
                    
                  
                } else {
                     //ENTRO AQUI SI ME QUEDA POCO PARA LLEGAR, SMOOTH CORNERING

                    
                    //probar 1000 coordenadas
                    int iterNum = 1000;
                    Point2D.Double cand;
                    for(int i =0; i < iterNum; i++){
                    
                           //i dont want it to be close to another bot, thats the meaning of  distanciaObjetivo*0.8
                            cand = hallarPunto(myPos, Math.min(distanciaObjetivo*0.8, 100 + iterNum*Math.random()), 2*Math.PI*Math.random());
                            if(perimetro.contains(cand) && evalHeuristic(cand) < evalHeuristic(futurePos)) {
                                    futurePos = cand;
                            }
                            
                          
                    } 
                    
                    prevPos = myPos;
                        
                }
        }
        
        
        //Heurístico para evaluar punto en el mapa
        // Si tiene mucha energia el bot enemigo, no nos gusta esa posicion
        
        //the lesser the better
        public static double evalHeuristic(Point2D.Double p) {
                
                double eval = 0.1/p.distanceSq(prevPos);
               
                Enumeration<BadBoy> enumVar = enemies.elements();
                while (enumVar.hasMoreElements()) {
                        BadBoy enemy = enumVar.nextElement();
                     
                        //how dangerous is enemy --> enemyeEnergy/ourEnergy
                        //
                        if(enemy.hp) {
                                eval += Math.min(enemy.energy/myEnergy,2) * 
                                                //evaluates riskiness of position                                       //anti gravity mov
                                                (1 + Math.abs(Math.cos(angulo(p, myPos) - angulo(enemy.pos, p)))) / p.distanceSq(enemy.pos);
                        }
                }
                return eval;
        }
        
        
        /**
         * Scan: Si no lo hemos avistado aun, lo incluimos en lista.
         */
        public void onScannedRobot(ScannedRobotEvent e)
        {
                BadBoy en = (BadBoy)enemies.get(e.getName());
              
                if(en == null){
                        en = new BadBoy();
                        enemies.put(e.getName(), en);
                }
                
               
                en.hp = true;
                en.energy = e.getEnergy();
                en.pos = hallarPunto(myPos, e.getDistance(), getHeadingRadians() + e.getBearingRadians());
                
                // normal target selection: the one closer to you is the most dangerous so attack him
                //si no le queda vida al objetivo actual CHANGE TARGET m8
                
                if(!objetivo.hp || e.getDistance() < myPos.distance(objetivo.pos)) {
                        objetivo = en;
                }
				
				double radarTurn =
									// Absolute bearing to target
									getHeadingRadians() + e.getBearingRadians()
									// Subtract current radar heading to get turn required
									- getRadarHeadingRadians();
				setTurnRadarRightRadians(2.0*Utils.normalRelativeAngle(radarTurn));
                
        }
        
        
        
        public void onRobotDeath(RobotDeathEvent e) {
                ((BadBoy)enemies.get(e.getName())).hp = false; //ESTA MUERTO, target cambiara en onScannedRobot proximo
        }
        
     
       
}
