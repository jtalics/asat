/* Copyright (c) 1986,1987,1988,1989,1990 Robert W. Berger N3EMO
   May be freely distributed, provided this notice remains intact. */

/* Change Log
	3/7/1990	v3.7 Make Phase III style phase (0-255) the default.
			Ignore case in satellite names.

	12/19/1989	v3.6 Use more direct calculations for dates.
			Calculate a new sidereal time reference for each run.

	12/8/1988	v3.5 Allow multiple overlapping modes in "mode.dat".

	6/28/1988	v3.4 Cleaned up Eclipse code. Fixed leap year handling
			for centesimal years. Added a heuristic to GetDay to
			allow 2 or 4 digit year specifications.
				  
	1/25/1988	v3.2 Rewrote orbitr.c to improve modularity,
			efficiency, and accuracy. Adopted geocentric
			cartesian coordinates as the standard representation
			for position and velocity. Added direct calculation
			 of range-rate for better doppler predections.

	12/1/1988	v3.1 Allow spaces in satellite names. Provide
			single character aliases for 62 satellites 
			(up from 26).

	4/7/87		v3.0 Added eclipses.

	4/1/87		v2.4 Added "Flip" option in site file for
			0-180 elevation support.

	3/24/87		v2.3 Adapted for new kepler.dat format.
			Allow beacon frequencies in mode.dat.
			Use decay rate for drag compensation.

	5/10/86		v2.2 Added single character aliases for satellite
			names.

	4/30/86		v2.1 Print blank line if satellite dips below
			horizon and reappears during same orbit and day

	4/29/86		v2.0 Changed GetSatelliteParams() to use AMSAT's
			"kepler.dat" file. Moved schedule to "mode.dat" file.

        4/22/86         v1.3  Inserted N8FJB's suggestions for variable naming
                        which maintain 8 character uniqueness.
                        Also removed "include" file orbitr.h, which had two
                        definitions of external functions defined in orbit.c
			    -K3MC 

        4/1/86          v1.2  Corrected a scanf conversion to %d for an int
                        type.    -K3MC
 
        3/19/86         v1.1  Changed GetSatelliteParams to not pass NULL
                        to sscanf.
                                                                        */
 
#define DRAG 1

#include <stdio.h>
#include <math.h>
#include <ctype.h>
 
extern double Kepler();
extern long GetDayNum();
 
#define LC(c) (isupper(c) ? tolower(c) : (c))

#ifndef PI
#define PI 3.14159265
#endif

#ifdef PI2
#undef PI2
#endif

#define PI2 (PI*2)

#define MinutesPerDay (24*60.0)
#define SecondsPerDay (60*MinutesPerDay)
#define HalfSecond (0.5/SecondsPerDay)
#define EarthRadius 6378.16             /* Kilometers           */
#define C 2.997925e5                    /* Kilometers/Second    */
#define TropicalYear 365.24199		/* Mean solar days	*/
#define EarthEccentricity 0.016713
#define DegreesPerRadian (180/PI)
#define RadiansPerDegree (PI/180)
#define ABS(x) ((x) < 0 ? (-(x)) : (x))
#define SQR(x) ((x)*(x))
 
#define MaxModes 10
typedef struct {
                int MinPhase,MaxPhase;
                char ModeStr[20];
               }  ModeRec;
 
char VersionStr[] = "N3EMO Orbit Simulator  v3.7";
 
    /*  Keplerian Elements and misc. data for the satellite              */
    double  EpochDay;                   /* time of epoch                 */
    double EpochMeanAnomaly;            /* Mean Anomaly at epoch         */
    long EpochOrbitNum;                 /* Integer orbit # of epoch      */
    double EpochRAAN;                   /* RAAN at epoch                 */
    double epochMeanMotion;             /* Revolutions/day               */
    double OrbitalDecay;                /* Revolutions/day^2             */
    double EpochArgPerigee;             /* argument of perigee at epoch  */
    double Eccentricity;
    double Inclination;
    char SatName[100];
    int ElementSet;
    double BeaconFreq;                  /* Mhz, used for doppler calc    */
    double MaxPhase;                    /* Phase units in 1 orbit        */
    double perigeePhase;
    int NumModes;
    ModeRec Modes[MaxModes];
    int PrintApogee;
    int PrintEclipses;
    int Flip;
 
    /* Simulation Parameters */
 
    double StartTime,EndTime, StepTime; /* In Days, 1 = New Year        */
                                        /*      of reference year       */
 
    /* Site Parameters */
    char SiteName[100];
    double SiteLat,SiteLong,SiteAltitude,SiteMinElev;
 
 
/* List the satellites in kepler.dat, and return the number found */
ListSatellites()
{
    char str[100];
    FILE *InFile;
    char satchar;
    int NumSatellites;

    printf("Available satellites:\n");

    if ((InFile = fopen("kepler.dat","r")) == 0)
        {
	printf("\"kepler.dat\" not found\n");
	exit(-1);
	}

    satchar = 'a';
    NumSatellites = 0;
    while (fgets(str,100,InFile))
	if (strncmp(str,"Satellite: ",11) == 0)
	    {
	    printf("	%c) %s",satchar,&str[11]);
	    if (satchar == 'z')
		satchar = 'A';
               else if (satchar == 'Z')
		   satchar = '0';
    	        else satchar++;
	    NumSatellites++;
	    }

    fclose(InFile);

    return NumSatellites;
}

/* Match and skip over a string in the input file. Exits on failure. */

MatchStr(InFile,FileName,Target)
FILE *InFile;
char *FileName,*Target;
{
    char str[100];

    fgets(str,strlen(Target)+1,InFile);
    if (strcmp(Target,str))
       {
       printf("%s: found \"%s\" while expecting \"%s\n\"",FileName,str,Target);
       exit(-1);
       }
}

LetterNum(c)
char c;
{
    if (c >= 'a' && c <= 'z')
	return c - 'a' + 1;
      else if (c >= 'A' && c <= 'Z')
 	  return c - 'A'+ 27;
	else if (c >= '0' && c <= '9')
	  return c - '0' + 53;
}
      
/* Case insensitive strncmp */
cstrncmp(str1,str2,l)
char *str1,*str2;
{
    int i;

    for (i = 0; i < l; i++)
	if (LC(str1[i]) != LC(str2[i]))
	    return 1;

    return 0;
}


cstrcmp(str1,str2)
char *str1,*str2;
{
    int i,l;

    l = strlen(str1);
    if (strlen(str2) != l)
	return 1;

    for (i = 0; i < l; i++)
	if (LC(str1[i]) != LC(str2[i]))
	    return 1;

    return 0;
}


GetSatelliteParams()
{
    FILE *InFile;
    char str[100];
    int EpochYear;
    double EpochHour,EpochMinute,EpochSecond;
    int found;
    int i,NumSatellites;
    char satchar;

    NumSatellites = ListSatellites();

    found = 0;

    while (!found)
	{
	printf("Letter or satellite name :");
	gets(SatName);

	if ((InFile = fopen("kepler.dat","r")) == 0)
	    {
	    printf("kepler.dat not found\n");
	    exit(-1);
	    }

	if (strlen(SatName) == 1)
	    {			/* use single character label */
	    satchar = SatName[0];
	    if (LetterNum(satchar) > NumSatellites)
	        {
	    	printf("'%c' is out of range\n",satchar);
		fclose(InFile);
		continue;
		}

	    for (i = 1; i <= LetterNum(satchar); i++)
		{
		do  /* find line beginning with "Satellite: " */
		    fgets(str,100,InFile);
		while (strncmp(str,"Satellite: ",11) != 0);
		}
	    found = 1;
	    strncpy(SatName,&str[11],strlen(str)-12);
	    }
		
	 else 
	     {
	     while (!found)  /* use satellite name */
            	{
	    	if (! fgets(str,100,InFile))
	    	    break;	/* EOF */

	    	if (strncmp(str,"Satellite: ",11) == 0)
		   if (cstrncmp(SatName,&str[11],strlen(SatName)) == 0)
			found = 1;
	        }

	    if (!found)
		{
		printf("Satellite %s not found\n",SatName);
		fclose(InFile);
		}
	    }
	}

    BeaconFreq = 146.0;  /* Default value */

    fgets(str,100,InFile);	/* Skip line */

    MatchStr(InFile,"kepler.dat","Epoch time:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&EpochDay);

    EpochYear = EpochDay / 1000.0;
    EpochDay -= EpochYear*1000.0;
    EpochDay += GetDayNum(EpochYear,1,0);
    fgets(str,100,InFile);

    if (sscanf(str,"Element set: %ld",&ElementSet) == 0)
       {   /* Old style kepler.dat */
       MatchStr(InFile,"kepler.dat","Element set:");
       fgets(str,100,InFile);
       sscanf(str,"%d",&ElementSet);
       }

    MatchStr(InFile,"kepler.dat","Inclination:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&Inclination);
    Inclination *= RadiansPerDegree;

    MatchStr(InFile,"kepler.dat","RA of node:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&EpochRAAN);
    EpochRAAN *= RadiansPerDegree;

    MatchStr(InFile,"kepler.dat","Eccentricity:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&Eccentricity);

    MatchStr(InFile,"kepler.dat","Arg of perigee:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&EpochArgPerigee);
    EpochArgPerigee *= RadiansPerDegree;

    MatchStr(InFile,"kepler.dat","Mean anomaly:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&EpochMeanAnomaly);
    EpochMeanAnomaly *= RadiansPerDegree;

    MatchStr(InFile,"kepler.dat","Mean motion:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&epochMeanMotion);

    MatchStr(InFile,"kepler.dat","Decay rate:");
    fgets(str,100,InFile);
    sscanf(str,"%lf",&OrbitalDecay);

    MatchStr(InFile,"kepler.dat","Epoch rev:");
    fgets(str,100,InFile);
    sscanf(str,"%ld",&EpochOrbitNum);

	while (1)
	    {
	    if (! fgets(str,100,InFile))
		break;	/* EOF */
	    if (strlen(str) <= 2)
	    	break;  /* Blank line */
	    sscanf(str,"Beacon: %lf",&BeaconFreq);
	    }

    PrintApogee = (Eccentricity >= 0.3);

    perigeePhase = 0; MaxPhase = 256; /* Default values */
    NumModes = 0;

    if ((InFile = fopen("mode.dat","r")) == 0)
	return;

    found = 0;
    while (!found)
        {
	if (! fgets(str,100,InFile))
	    break;	/* EOF */
	if (sscanf(str,"Satellite: %s",str) == 1
	    && cstrcmp(SatName,str) == 0)
		found = 1;
	}
	
    if (found)
	{
	while (1)
	    {
	    if (! fgets(str,100,InFile))
		break;	/* EOF */
	    if (strlen(str) <= 2)
	    	break;  /* Blank line */
	    sscanf(str,"Beacon: %lf",&BeaconFreq);
	    sscanf(str,"Perigee phase: %lf",&perigeePhase);
	    sscanf(str,"Max phase: %lf",&MaxPhase);

	    if (sscanf(str,"Mode: %20s from %d to %d",Modes[NumModes].ModeStr,
	    &Modes[NumModes].MinPhase,&Modes[NumModes].MaxPhase) == 3
	      && NumModes < MaxModes)
		  NumModes++;
	    }
	fclose(InFile);
	}
}

 
GetSiteParams()
{
    FILE *InFile;
    char name[100],str[100];
 
    printf("Site name :");
    gets(name);
    strcat(name,".sit");
 
    if ((InFile = fopen(name,"r")) == 0)
        {
        printf("%s not found\n",name);
        exit(-1);
        }
 
    fgets(SiteName,100,InFile);
 
    fgets(str,100,InFile);
    sscanf(str,"%lf",&SiteLat);
    SiteLat *= RadiansPerDegree;
 
    fgets(str,100,InFile);
    sscanf(str,"%lf",&SiteLong);
    SiteLong *= RadiansPerDegree;
 
    fgets(str,100,InFile);
    sscanf(str,"%lf",&SiteAltitude);
    SiteAltitude /= 1000;   /* convert to km */
 
    fgets(str,100,InFile);
    sscanf(str,"%lf",&SiteMinElev);
    SiteMinElev *= RadiansPerDegree;

    Flip = PrintEclipses = 0;
    while (fgets(str,100,InFile))
	{
	if (strncmp(str,"Flip",4) == 0)
	    Flip = 1;
	  else if (strncmp(str,"Eclipse",7) == 0)
	    PrintEclipses = 1;
	   else printf("\"%s\" unknown option: %s",name,str);
	}
}
 
GetSimulationParams()
{
    double hour,duration;
    int Month,Day,Year;
 
    printf("Start date (UTC) (Month Day Year) :");
    scanf("%d%d%d",&Month,&Day,&Year);
 
    StartTime = GetDayNum(Year,Month,Day);
    printf("Starting Hour (UTC) :");
    scanf("%lf",&hour);
    StartTime += hour/24;
 
    printf("Duration (Days) :");
    scanf("%lf",&duration);
    EndTime = StartTime + duration;
 
    printf("Time Step (Minutes) :");
    scanf("%lf",&StepTime);
    StepTime /= MinutesPerDay;
}
 
PrintMode(OutFile,Phase)
FILE *OutFile;
{
    int CurMode;
 
    for (CurMode = 0; CurMode < NumModes; CurMode++)
        if ((Phase >= Modes[CurMode].MinPhase
                && Phase < Modes[CurMode].MaxPhase)
              || ((Modes[CurMode].MinPhase > Modes[CurMode].MaxPhase)
                  && (Phase >= Modes[CurMode].MinPhase
                        || Phase < Modes[CurMode].MaxPhase)))
            {
            fprintf(OutFile,"%s ",Modes[CurMode].ModeStr);
            }
}
 
 
main()
{
    double ReferenceOrbit;      /* Floating point orbit # at epoch */
    double CurrentTime;         /* In Days                         */
    double CurrentOrbit;
    double AverageMotion,       /* Corrected for drag              */
        CurrentMotion;
    double MeanAnomaly,TrueAnomaly;
    double SemiMajorAxis;
    double Radius;              /* From geocenter                  */
    double SatX,SatY,SatZ;	/* In Right Ascension based system */
    double SatVX,SatVY,SatVZ;   /* Kilometers/second		   */
    double SiteX,SiteY,SiteZ;
    double SiteVX,SiteVY;
    double SiteMatrix[3][3];
    double Height;
    double RAANPrecession,PerigeePrecession;
    double SSPLat,SSPLong;
    long OrbitNum,PrevOrbitNum;
    long Day,PrevDay;
    double Azimuth,Elevation,Range;
    double RangeRate,Doppler;
    int Phase;
    char FileName[100];
    FILE *OutFile;
    int DidApogee;
    double TmpTime,PrevTime;
    int PrevVisible;

    printf("%s\n",VersionStr);
 

    GetSatelliteParams();
    GetSiteParams();
    GetSimulationParams();
 
    InitOrbitRoutines((StartTime+EndTime)/2);

    printf("Output file (RETURN for TTY) :");
    gets(FileName);     /* Skip previous RETURN */
    gets(FileName);
 
 
    if (strlen(FileName) > 0)
        {
        if ((OutFile = fopen(FileName,"w")) == 0)
            {
            printf("Can't write to %s\n",FileName);
            exit(-1);
            }
        }
      else OutFile = stdout;
 
    fprintf(OutFile,"%s Element Set %d\n",SatName,ElementSet);

    fprintf(OutFile,"%s\n",SiteName);
 
    fprintf(OutFile,"Doppler calculated for freq = %lf MHz\n",BeaconFreq);
 
    SemiMajorAxis = 331.25 * exp(2*log(MinutesPerDay/epochMeanMotion)/3);
    GetPrecession(SemiMajorAxis,Eccentricity,Inclination,&RAANPrecession,
                        &PerigeePrecession);

    ReferenceOrbit = EpochMeanAnomaly/PI2 + EpochOrbitNum;
 
    PrevDay = -10000; PrevOrbitNum = -10000;
    PrevTime = StartTime-2*StepTime;
 
    BeaconFreq *= 1E6;          /* Convert to Hz */
 
    DidApogee = 0;
 
    for (CurrentTime = StartTime; CurrentTime <= EndTime;
                CurrentTime += StepTime)
        {
 
        AverageMotion = epochMeanMotion
	   + (CurrentTime-EpochDay)*OrbitalDecay/2;
        CurrentMotion = epochMeanMotion
	   + (CurrentTime-EpochDay)*OrbitalDecay;

        SemiMajorAxis = 331.25 * exp(2*log(MinutesPerDay/CurrentMotion)/3);
 
        CurrentOrbit = ReferenceOrbit +
                        (CurrentTime-EpochDay)*AverageMotion;
        OrbitNum = CurrentOrbit;
 
        MeanAnomaly = (CurrentOrbit-OrbitNum)*PI2;
 
        TmpTime = CurrentTime;
        if (MeanAnomaly < PI)
            DidApogee = 0;
        if (PrintApogee && !DidApogee && MeanAnomaly > PI)
            {                   /* Calculate Apogee */
            TmpTime -= StepTime;   /* So we pick up later where we left off */
            MeanAnomaly = PI;
            CurrentTime=EpochDay+(OrbitNum-ReferenceOrbit+0.5)/AverageMotion;
            }
 
        TrueAnomaly = Kepler(MeanAnomaly,Eccentricity);

	GetSatPosition(EpochDay,EpochRAAN,EpochArgPerigee,SemiMajorAxis,
	    Inclination,Eccentricity,RAANPrecession,PerigeePrecession,
	    CurrentTime,TrueAnomaly,&SatX,&SatY,&SatZ,&Radius,
	    &SatVX,&SatVY,&SatVZ);

	GetSitPosition(SiteLat,SiteLong,SiteAltitude,CurrentTime,
		&SiteX,&SiteY,&SiteZ,&SiteVX,&SiteVY,SiteMatrix);


	GetBearings(SatX,SatY,SatZ,SiteX,SiteY,SiteZ,SiteMatrix,
		&Azimuth,&Elevation);

 
        if (Elevation >= SiteMinElev && CurrentTime >= StartTime)
            {

            Day = CurrentTime + HalfSecond;
            if (((double) Day) > CurrentTime+HalfSecond)
                Day -= 1;    /* Correct for truncation of negative values */

	    if (OrbitNum == PrevOrbitNum && Day == PrevDay && !PrevVisible)
	    	fprintf(OutFile,"\n");	/* Dipped out of sight; print blank */

            if (OrbitNum != PrevOrbitNum || Day != PrevDay)
                {                       /* Print Header */
		PrintDayOfWeek(OutFile,(long) Day);
		fprintf(OutFile," ");
                PrintDate(OutFile,(long) Day);
                fprintf(OutFile,"  ----Orbit # %ld-----\n",OrbitNum);
                fprintf(OutFile," U.T.C.   Az  El  ");
		if (Flip)
                    fprintf(OutFile," Az'  El' ");

		fprintf(OutFile,"Doppler Range");
                fprintf(OutFile," Height  Lat  Long  Phase(%3.0lf)\n",
                                MaxPhase);
                }
            PrevOrbitNum = OrbitNum; PrevDay = Day;
            PrintTime(OutFile,CurrentTime + HalfSecond);
 
            fprintf(OutFile,"  %3.0lf %3.0lf",Azimuth*DegreesPerRadian,
                Elevation*DegreesPerRadian);
	    if (Flip)
		{
		Azimuth += PI;
		if (Azimuth >= PI2)
		    Azimuth -= PI2;
		Elevation = PI-Elevation;
		fprintf(OutFile,"  %3.0lf  %3.0lf",Azimuth*DegreesPerRadian,
			Elevation*DegreesPerRadian);
		}

  	    GetRange(SiteX,SiteY,SiteZ,SiteVX,SiteVY,
		SatX,SatY,SatZ,SatVX,SatVY,SatVZ,&Range,&RangeRate);
            Doppler = -BeaconFreq*RangeRate/C;
            fprintf(OutFile,"  %6.0lf %6.0lf",Doppler,Range);
 
	    GetSubSatPoint(SatX,SatY,SatZ,CurrentTime,
	        &SSPLat,&SSPLong,&Height);
            fprintf(OutFile," %6.0lf  %3.0lf  %4.0lf",
                Height,SSPLat*DegreesPerRadian,
                SSPLong*DegreesPerRadian);
 
            Phase = (MeanAnomaly/PI2*MaxPhase + perigeePhase);
            while (Phase < 0)
                Phase += MaxPhase;
            while (Phase >= MaxPhase)
                Phase -= MaxPhase;
 
            fprintf(OutFile," %4d  ", Phase);
            PrintMode(OutFile,Phase);

            if (PrintApogee && (MeanAnomaly == PI))
                fprintf(OutFile,"    Apogee");

	    if (PrintEclipses)
		if (Eclipsed(SatX,SatY,SatZ,Radius,CurrentTime))
		    fprintf(OutFile,"  Eclipse");

            fprintf(OutFile,"\n");
	    PrevVisible = 1;
            }
	 else
	    PrevVisible = 0;	
        if (PrintApogee && (MeanAnomaly == PI))
            DidApogee = 1;


        PrevTime = CurrentTime;
        CurrentTime = TmpTime;
        }
    fclose(OutFile);
}
