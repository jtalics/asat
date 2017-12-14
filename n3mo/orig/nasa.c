/* nasa.c   convert file of NASA keplerians to AMSAT format
 7/12/87  Robert W. Berger N3EMO			*/

/* 11/30/88	v3.5	Incorporate Greg Troxel's fix for reading epoch
			times with imbedded spaces		*/

#include <stdio.h>

main()
{   char SatName[100],line1[100],line2[100];
    FILE *InFile,*OutFile;
    int LineNum,SatNum,ElementSet,EpochRev;
    double EpochDay,DecayRate,Inclination,RAAN,Eccentricity;
    double ArgPerigee,MeanAnomaly,MeanMotion;
    double EpochYear;

    if ((InFile = fopen("nasabare.txt","r")) == 0)
        {
	printf("\"nasabare.txt\" not found\n");
	exit(-1);
	}

    if ((OutFile = fopen("kepler.dat","w")) == 0)
        {
	printf("Can't write \"kepler.dat\"\n");
	exit(-1);
	}


    while (fgets(SatName,100,InFile))
	{
	printf("%s",SatName);
	fgets(line1,100,InFile);
	fgets(line2,100,InFile);
	
        sscanf(line1,"%1d",&LineNum);
  	if (LineNum != 1)
	    {
	    printf("Line 1 not present for satellite %s",SatName);
	    exit(-1);
	    }
        sscanf(line2,"%1d",&LineNum);
  	if (LineNum != 2)
	    {
	    printf("Line 2 not present for satellite %s",SatName);
	    exit(-1);
	    }

	sscanf(line1,"%*2c%5d%*10c%2lf%12lf%10lf%*21c%5d",
		&SatNum,&EpochYear,&EpochDay,&DecayRate,&ElementSet);
	EpochDay += EpochYear *1000;

	ElementSet /= 10;   /* strip off checksum */

	sscanf(line2,"%*8c%8lf%8lf%7lf%8lf%8lf%11lf%5d",
	   &Inclination,&RAAN,&Eccentricity,&ArgPerigee,&MeanAnomaly,
	   &MeanMotion,&EpochRev);
	EpochRev /= 10;   /* strip off checksum */
	Eccentricity *= 1E-7;


	fprintf(OutFile,"Satellite: %s",SatName);
	fprintf(OutFile,"Catalog number: %d\n",SatNum);
	fprintf(OutFile,"Epoch time: %lf\n",EpochDay);
	fprintf(OutFile,"Element set: %d\n",ElementSet);
	fprintf(OutFile,"Inclination: %lf deg\n",Inclination);
	fprintf(OutFile,"RA of node: %lf deg\n",RAAN);
	fprintf(OutFile,"Eccentricity: %lf\n",Eccentricity);
	fprintf(OutFile,"Arg of perigee: %lf deg\n",ArgPerigee);
	fprintf(OutFile,"Mean anomaly: %lf deg\n",MeanAnomaly);
	fprintf(OutFile,"Mean motion: %lf rev/day\n",MeanMotion);
	fprintf(OutFile,"Decay rate: %le rev/day^2\n",DecayRate);
	fprintf(OutFile,"Epoch rev: %d\n",EpochRev);
	fprintf(OutFile,"\n");
	}

    fclose(InFile);
    fclose(OutFile);

}
		
