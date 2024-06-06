#include <mpi.h>
#include <stdio.h>
#include <math.h>

int main(int argc, char *argv[])
{
	int oldrank,oldsize;
	int newrank,newsize;

	MPI_Comm newcomm;

        MPI_Init(&argc, &argv); 

	MPI_Comm_rank(MPI_COMM_WORLD, &oldrank);
    	MPI_Comm_size(MPI_COMM_WORLD, &oldsize);
 
	MPI_Comm_split(MPI_COMM_WORLD, oldrank%2, oldrank, &newcomm);
 
	MPI_Comm_rank(newcomm, &newrank);
    	MPI_Comm_size(newcomm, &newsize);
 
	printf( "old rank = %d size = %d, color = %d , new rank = %d size = %d\n", oldrank, oldsize, oldrank%2, newrank, newsize);

        MPI_Finalize();
        return 0;
}
