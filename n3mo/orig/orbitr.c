/* N3EMO Orbit Simulator routines  v3.7 */

/* Copyright (c) 1986,1987,1988,1989,1990 Robert W. Berger N3EMO
   May be freely distributed, provided this notice remains intact. */

#include <stdio.h>
#include <math.h>
 
#define SSPELLIPSE 0		/* If non zero, use ellipsoidal earth model
				   when calculating longitude, latitude, and
				   height */

#ifndef PI
#define PI 3.14159265
#endif

#ifdef PI2
#undef PI2
#endif

#ifdef E
#undef E
#endif

typedef double mat3x3[3][3];

#define PI2 (PI*2)
#define MinutesPerDay (24*60.0)
#define SecondsPerDay (60*MinutesPerDay)
#define HalfSecond (0.5/SecondsPerDay)
#define EarthRadius 6378.16             /* Kilometers, at equator */

#define EarthFlat (1/298.25)            /* Earth Flattening Coeff. */
#define SiderealSolar 1.0027379093
#define SidRate (PI2*SiderealSolar/SecondsPerDay)	/* radians/second */
#define GM 398600			/* Kilometers^3/seconds^2 */
#define DegreesPerRadian (180/PI)
#define RadiansPerDegree (PI/180)
#define ABS(x) ((x) < 0 ? (-(x)) : (x))
#define SQR(x) ((x)*(x))
 
#define Epsilon (RadiansPerDegree/3600)     /* 1 arc second */
#define SunRadius 695000		
#define SunSemiMajorAxis  149598845.0  	    /* Kilometers 		   */


double SidDay,SidReference;	/* Date and sidereal time	*/

/* Keplerian elements for the sun */
double SunEpochTime,SunInclination,SunRAAN,SunEccentricity,
       SunArgPerigee,SunMeanAnomaly,SunMeanMotion;

/* values for shadow geometry */
double SinPenumbra,CosPenumbra;

 
/* Solve Kepler's equation                                      */
/* Inputs:                                                      */
/*      MeanAnomaly     Time Since last perigee, in radians.    */
/*                      PI2 = one complete orbit.               */
/*      Eccentricity    Eccentricity of orbit's ellipse.        */
/* Output:                                                      */
/*      TrueAnomaly     Angle between perigee, geocenter, and   */
/*                      current position.                       */
 
long calls = 0;
long iters = 0;

dumpstats()
{
printf("Average iterations = %lf\n",((double) iters)/calls);
}

double Kepler(MeanAnomaly,Eccentricity)
register double MeanAnomaly,Eccentricity;
 
{
register double E;              /* Eccentric Anomaly                    */
register double Error;
register double TrueAnomaly;
 
calls++;

    E = MeanAnomaly ;/*+ Eccentricity*sin(MeanAnomaly);   /* Initial guess */
    do
        {
        Error = (E - Eccentricity*sin(E) - MeanAnomaly)
                / (1 - Eccentricity*cos(E));
        E -= Error;
iters++;
        }
   while (ABS(Error) >= Epsilon);

    if (ABS(E-PI) < Epsilon)
        TrueAnomaly = PI;
      else
        TrueAnomaly = 2*atan(sqrt((1+Eccentricity)/(1-Eccentricity))
                                *tan(E/2));
    if (TrueAnomaly < 0)
        TrueAnomaly += PI2;
 
    return TrueAnomaly;
}
 
GetSubSatPoint(SatX,SatY,SatZ,Time,Latitude,Longitude,Height)
double SatX,SatY,SatZ,Time;
double *Latitude,*Longitude,*Height;
{
    double r;
    long i;

    r = sqrt(SQR(SatX) + SQR(SatY) + SQR(SatZ));

    *Longitude = PI2*((Time-SidDay)*SiderealSolar + SidReference)
		    - atan2(SatY,SatX);

    /* i = floor(Longitude/2*pi)        */
    i = *Longitude/PI2;
    if(i < 0)
        i--;
 
    *Longitude -= i*PI2;

    *Latitude = atan(SatZ/sqrt(SQR(SatX) + SQR(SatY)));

#if SSPELLIPSE
#else
    *Height = r - EarthRadius;
#endif
}
 
 
GetPrecession(SemiMajorAxis,Eccentricity,Inclination,
        RAANPrecession,PerigeePrecession)
double SemiMajorAxis,Eccentricity,Inclination;
double *RAANPrecession,*PerigeePrecession;
{
  *RAANPrecession = 9.95*pow(EarthRadius/SemiMajorAxis,3.5) * cos(Inclination)
                 / SQR(1-SQR(Eccentricity)) * RadiansPerDegree;
 
  *PerigeePrecession = 4.97*pow(EarthRadius/SemiMajorAxis,3.5)
         * (5*SQR(cos(Inclination))-1)
                 / SQR(1-SQR(Eccentricity)) * RadiansPerDegree;
}
 
/* Compute the satellite postion and velocity in the RA based coordinate
   system */

GetSatPosition(EpochTime,EpochRAAN,EpochArgPerigee,SemiMajorAxis,
	Inclination,Eccentricity,RAANPrecession,PerigeePrecession,
	Time,TrueAnomaly,X,Y,Z,Radius,VX,VY,VZ)
 
double EpochTime,EpochRAAN,EpochArgPerigee;
double SemiMajorAxis,Inclination,Eccentricity;
double RAANPrecession,PerigeePrecession,Time, TrueAnomaly;
double *X,*Y,*Z,*Radius,*VX,*VY,*VZ;

{
    double RAAN,ArgPerigee;
 

    double Xw,Yw,VXw,VYw;	/* In orbital plane */
    double Tmp;
    double Px,Qx,Py,Qy,Pz,Qz;	/* Escobal transformation 31 */
    double CosArgPerigee,SinArgPerigee;
    double CosRAAN,SinRAAN,CoSinclination,SinInclination;

        *Radius = SemiMajorAxis*(1-SQR(Eccentricity))
                        / (1+Eccentricity*cos(TrueAnomaly));


    Xw = *Radius * cos(TrueAnomaly);
    Yw = *Radius * sin(TrueAnomaly);
    
    Tmp = sqrt(GM/(SemiMajorAxis*(1-SQR(Eccentricity))));

    VXw = -Tmp*sin(TrueAnomaly);
    VYw = Tmp*(cos(TrueAnomaly) + Eccentricity);

    ArgPerigee = EpochArgPerigee + (Time-EpochTime)*PerigeePrecession;
    RAAN = EpochRAAN - (Time-EpochTime)*RAANPrecession;

    CosRAAN = cos(RAAN); SinRAAN = sin(RAAN);
    CosArgPerigee = cos(ArgPerigee); SinArgPerigee = sin(ArgPerigee);
    CoSinclination = cos(Inclination); SinInclination = sin(Inclination);
    
    Px = CosArgPerigee*CosRAAN - SinArgPerigee*SinRAAN*CoSinclination;
    Py = CosArgPerigee*SinRAAN + SinArgPerigee*CosRAAN*CoSinclination;
    Pz = SinArgPerigee*SinInclination;
    Qx = -SinArgPerigee*CosRAAN - CosArgPerigee*SinRAAN*CoSinclination;
    Qy = -SinArgPerigee*SinRAAN + CosArgPerigee*CosRAAN*CoSinclination;
    Qz = CosArgPerigee*SinInclination;

    *X = Px*Xw + Qx*Yw;		/* Escobal, transformation #31 */
    *Y = Py*Xw + Qy*Yw;
    *Z = Pz*Xw + Qz*Yw;

    *VX = Px*VXw + Qx*VYw;
    *VY = Py*VXw + Qy*VYw;
    *VZ = Pz*VXw + Qz*VYw;
}

/* Compute the site postion and velocity in the RA based coordinate
   system. SiteMatrix is set to a matrix which is used by GetTopoCentric
   to convert geocentric coordinates to topocentric (observer-centered)
    coordinates. */

GetSitPosition(SiteLat,SiteLong,SiteElevation,CurrentTime,
             SiteX,SiteY,SiteZ,SiteVX,SiteVY,SiteMatrix)

double SiteLat,SiteLong,SiteElevation,CurrentTime;
double *SiteX,*SiteY,*SiteZ,*SiteVX,*SiteVY;
mat3x3 SiteMatrix;

{
    static double G1,G2; /* Used to correct for flattening of the Earth */
    static double CosLat,SinLat;
    static double OldSiteLat = -100000;  /* Used to avoid unneccesary recomputation */
    static double OldSiteElevation = -100000;
    double Lat;
    double SiteRA;	/* Right Ascension of site			*/
    double CosRA,SinRA;

    if ((SiteLat != OldSiteLat) || (SiteElevation != OldSiteElevation))
	{
	OldSiteLat = SiteLat;
	OldSiteElevation = SiteElevation;
	Lat = atan(1/(1-SQR(EarthFlat))*tan(SiteLat));

	CosLat = cos(Lat);
	SinLat = sin(Lat);

	G1 = EarthRadius/(sqrt(1-(2*EarthFlat-SQR(EarthFlat))*SQR(SinLat)));
	G2 = G1*SQR(1-EarthFlat);
	G1 += SiteElevation;
	G2 += SiteElevation;
	}


    SiteRA = PI2*((CurrentTime-SidDay)*SiderealSolar + SidReference)
	         - SiteLong;
    CosRA = cos(SiteRA);
    SinRA = sin(SiteRA);
    

    *SiteX = G1*CosLat*CosRA;
    *SiteY = G1*CosLat*SinRA;
    *SiteZ = G2*SinLat;
    *SiteVX = -SidRate * *SiteY;
    *SiteVY = SidRate * *SiteX;

    SiteMatrix[0][0] = SinLat*CosRA;
    SiteMatrix[0][1] = SinLat*SinRA;
    SiteMatrix[0][2] = -CosLat;
    SiteMatrix[1][0] = -SinRA;
    SiteMatrix[1][1] = CosRA;
    SiteMatrix[1][2] = 0.0;
    SiteMatrix[2][0] = CosRA*CosLat;
    SiteMatrix[2][1] = SinRA*CosLat;
    SiteMatrix[2][2] = SinLat;
}

GetRange(SiteX,SiteY,SiteZ,SiteVX,SiteVY,
	SatX,SatY,SatZ,SatVX,SatVY,SatVZ,Range,RangeRate)

double SiteX,SiteY,SiteZ,SiteVX,SiteVY;
double SatX,SatY,SatZ,SatVX,SatVY,SatVZ;
double *Range,*RangeRate;
{
    double DX,DY,DZ;

    DX = SatX - SiteX; DY = SatY - SiteY; DZ = SatZ - SiteZ;

    *Range = sqrt(SQR(DX)+SQR(DY)+SQR(DZ));    

    *RangeRate = ((SatVX-SiteVX)*DX + (SatVY-SiteVY)*DY + SatVZ*DZ)
			/ *Range;
}

/* Convert from geocentric RA based coordinates to topocentric
   (observer centered) coordinates */

GetTopocentric(SatX,SatY,SatZ,SiteX,SiteY,SiteZ,SiteMatrix,X,Y,Z)
double SatX,SatY,SatZ,SiteX,SiteY,SiteZ;
double *X,*Y,*Z;
mat3x3 SiteMatrix;
{
    SatX -= SiteX;
    SatY -= SiteY;
    SatZ -= SiteZ;

    *X = SiteMatrix[0][0]*SatX + SiteMatrix[0][1]*SatY
	+ SiteMatrix[0][2]*SatZ; 
    *Y = SiteMatrix[1][0]*SatX + SiteMatrix[1][1]*SatY
	+ SiteMatrix[1][2]*SatZ; 
    *Z = SiteMatrix[2][0]*SatX + SiteMatrix[2][1]*SatY
	+ SiteMatrix[2][2]*SatZ; 
}

GetBearings(SatX,SatY,SatZ,SiteX,SiteY,SiteZ,SiteMatrix,Azimuth,Elevation)
double SatX,SatY,SatZ,SiteX,SiteY,SiteZ;
mat3x3 SiteMatrix;
double *Azimuth,*Elevation;
{
    double x,y,z;

    GetTopocentric(SatX,SatY,SatZ,SiteX,SiteY,SiteZ,SiteMatrix,&x,&y,&z);

    *Elevation = atan(z/sqrt(SQR(x) + SQR(y)));

    *Azimuth = PI - atan2(y,x);

    if (*Azimuth < 0)
	*Azimuth += PI;
}

Eclipsed(SatX,SatY,SatZ,SatRadius,CurrentTime)
double SatX,SatY,SatZ,SatRadius,CurrentTime;
{
    double MeanAnomaly,TrueAnomaly;
    double SunX,SunY,SunZ,SunRad;
    double vx,vy,vz;
    double CosTheta;

    MeanAnomaly = SunMeanAnomaly+ (CurrentTime-SunEpochTime)*SunMeanMotion*PI2;
    TrueAnomaly = Kepler(MeanAnomaly,SunEccentricity);

    GetSatPosition(SunEpochTime,SunRAAN,SunArgPerigee,SunSemiMajorAxis,
		SunInclination,SunEccentricity,0.0,0.0,CurrentTime,
		TrueAnomaly,&SunX,&SunY,&SunZ,&SunRad,&vx,&vy,&vz);

    CosTheta = (SunX*SatX + SunY*SatY + SunZ*SatZ)/(SunRad*SatRadius)
		 *CosPenumbra + (SatRadius/EarthRadius)*SinPenumbra;

    if (CosTheta < 0)
        if (CosTheta < -sqrt(SQR(SatRadius)-SQR(EarthRadius))/SatRadius
	    		*CosPenumbra + (SatRadius/EarthRadius)*SinPenumbra)
	  
	    return 1;
    return 0;
}

/* Initialize the Sun's keplerian elements for a given epoch.
   Formulas are from "Explanatory Supplement to the Astronomical Ephemeris".
   Also init the sidereal reference				*/

InitOrbitRoutines(EpochDay)
double EpochDay;
{
    double T,T2,T3,Omega;
    int n;
    double SunTrueAnomaly,SunDistance;

    T = (floor(EpochDay)-0.5)/36525;
    T2 = T*T;
    T3 = T2*T;

    SidDay = floor(EpochDay);

    SidReference = (6.6460656 + 2400.051262*T + 0.00002581*T2)/24;
    SidReference -= floor(SidReference);

    /* Omega is used to correct for the nutation and the abberation */
    Omega = (259.18 - 1934.142*T) * RadiansPerDegree;
    n = Omega / PI2;
    Omega -= n*PI2;

    SunEpochTime = EpochDay;
    SunRAAN = 0;

    SunInclination = (23.452294 - 0.0130125*T - 0.00000164*T2
		    + 0.000000503*T3 +0.00256*cos(Omega)) * RadiansPerDegree;
    SunEccentricity = (0.01675104 - 0.00004180*T - 0.000000126*T2);
    SunArgPerigee = (281.220833 + 1.719175*T + 0.0004527*T2
			+ 0.0000033*T3) * RadiansPerDegree;
    SunMeanAnomaly = (358.475845 + 35999.04975*T - 0.00015*T2
			- 0.00000333333*T3) * RadiansPerDegree;
    n = SunMeanAnomaly / PI2;
    SunMeanAnomaly -= n*PI2;

    SunMeanMotion = 1/(365.24219879 - 0.00000614*T);

    SunTrueAnomaly = Kepler(SunMeanAnomaly,SunEccentricity);
    SunDistance = SunSemiMajorAxis*(1-SQR(SunEccentricity))
			/ (1+SunEccentricity*cos(SunTrueAnomaly));

    SinPenumbra = (SunRadius-EarthRadius)/SunDistance;
    CosPenumbra = sqrt(1-SQR(SinPenumbra));
}

 
SPrintTime(Str,Time)
char *Str;
double Time;
{
    int day,hours,minutes,seconds;
 
    day = Time;
    Time -= day;
    if (Time < 0)
        Time += 1.0;   /* Correct for truncation problems with negatives */
 
    hours = Time*24;
    Time -=  hours/24.0;
 
    minutes = Time*MinutesPerDay;
    Time -= minutes/MinutesPerDay;
 
    seconds = Time*SecondsPerDay;
    seconds -= seconds/SecondsPerDay;
 
    sprintf(Str,"%02d%02d:%02d",hours,minutes,seconds);
}
 
PrintTime(OutFile,Time)
FILE *OutFile;
double Time;
{
    char str[100];

    SPrintTime(str,Time);
    fprintf(OutFile,"%s",str);
}


/* Get the Day Number for a given date. January 1 of the reference year
   is day 0. Note that the Day Number may be negative, if the sidereal
   reference is in the future.                                          */
 
/* Date calculation routines
  Robert Berger @ Carnegie Mellon

  January 1, 1900 is day 0
  valid from 1900 through 2099 */

/* #include <stdio.h> */

char *MonthNames[] = { "Jan","Feb","Mar","Apr","May","Jun","Jul",
                        "Aug","Sep","Oct","Nov","Dec" };
 
int MonthDays[] = {0,31,59,90,120,151,181,212,243,273,304,334};
		
 
char *DayNames[] = { "Sunday","Monday","Tuesday","Wednesday","Thursday",
                        "Friday","Saturday"};


long GetDayNum(Year,Month,Day)
{
    long Result;
    
    /* Heuristic to allow 4 or 2 digit year specifications */
    if (Year < 50)
	Year += 2000;
      else if (Year < 100)
	Year += 1900;
	
    Result = ((((long) Year-1901)*1461)>>2) + MonthDays[Month-1] + Day + 365;
    if (Year%4 == 0 && Month > 2)
        Result++;

    return Result;
}

GetDate(DayNum,Year,Month,Day)
long DayNum;
int *Year,*Month,*Day;    
{
    int M,L;
    long Y;
	
    Y = 4*DayNum;
    Y /= 1461;

    DayNum =  DayNum -365 - (((Y-1)*1461)>>2);
    
    L = 0;
    if (Y%4 == 0 && DayNum > MonthDays[2])
        L = 1;
	
    M = 1;
	     
    while (DayNum > MonthDays[M]+L)
	M++;
	
    DayNum -= (MonthDays[M-1]);
    if (M > 2)
        DayNum -= L;
   
    *Year = Y+1900;
    *Month = M;
    *Day = DayNum;
}    

/* Sunday = 0 */
GetDayOfWeek(DayNum)
long DayNum;
{
    return DayNum % 7;
}    

SPrintDate(Str,DayNum)
char *Str;
long DayNum;
{
    int Month,Day,Year;
    
    GetDate(DayNum,&Year,&Month,&Day);
    sprintf(Str,"%d %s %d",Day,
                MonthNames[Month-1],Year);
} 

SPrintDayOfWeek(Str,DayNum)
char *Str;
long DayNum;
{
    strcpy(Str,DayNames[DayNum%7]);
}

PrintDate(OutFile,DayNum)
FILE *OutFile;
long DayNum;
{
    char str[100];

    SPrintDate(str,DayNum);
    fprintf(OutFile,"%s",str);
}

PrintDayOfWeek(OutFile,DayNum)
FILE *OutFile;
long DayNum;
{
    fprintf(OutFile,"%s",DayNames[DayNum%7]);
}    
  
