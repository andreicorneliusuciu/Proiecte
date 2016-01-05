    #include <stdio.h>
    #include <math.h>
    #include <malloc.h>
    #include <unistd.h>
    #include <string.h>
    #include <pthread.h>
    #define NUM_COLORS 256
    #define NUM_THREADS 4
    #include <stdint.h>

typedef struct
{
    unsigned char r;
    unsigned char g;
    unsigned char b;
} Pixel;

typedef struct 
{
    int width;
    int height;
    Pixel *pixels;
} ImageRGB;


typedef struct
{
    ImageRGB *imageRGB;
    double x_min;
    double x_max;
    double y_min;
    double y_max;
    double resolution;
    int steps;
    double cx;
    double cy;
    int width;
    int height;
    int threadId;
} Params;

ImageRGB* finalImageRGB;
pthread_mutex_t lock;

/* HSV to RGB conversion function with only integer
 * math */
void hsvtorgb(unsigned char *r, unsigned char *g, unsigned char *b, unsigned char h, unsigned char s, unsigned char v)
{
    unsigned char region, fpart, p, q, t;
    
    if(s == 0) {
        /* color is grayscale */
        *r = *g = *b = v;
        return;
    }
    
    /* make hue 0-5 */
    region = h / 43;
    /* find remainder part, make it from 0-255 */
    fpart = (h - (region * 43)) * 6;
    
    /* calculate temp vars, doing integer multiplication */
    p = (v * (255 - s)) >> 8;
    q = (v * (255 - ((s * fpart) >> 8))) >> 8;
    t = (v * (255 - ((s * (255 - fpart)) >> 8))) >> 8;
        
    /* assign temp vars based on color cone region */
    switch(region) {
        case 0:
            *r = v; *g = t; *b = p; break;
        case 1:
            *r = q; *g = v; *b = p; break;
        case 2:
            *r = p; *g = v; *b = t; break;
        case 3:
            *r = p; *g = q; *b = v; break;
        case 4:
            *r = t; *g = p; *b = v; break;
        default:
            *r = v; *g = p; *b = q; break;
    }
    
    return;
}

void read_file(char *input_file, int* set_type, double* x_min, double* x_max , double* y_min, double* y_max, double* resolution, int* steps, double *cx, double *cy)
{
    FILE* file;
    file = fopen(input_file,"rt");

    if (file == NULL)
    {
        printf("Error Reading File\n");
    }

    fscanf(file, "%d", set_type);
    fscanf(file, "%lf%lf%lf%lf", x_min, x_max , y_min, y_max);
    fscanf(file, "%lf", resolution);
    fscanf(file, "%d", steps);
    fscanf(file, "%lf%lf", cx, cy);

    fclose(file);
}

void write_file(char *output_file, ImageRGB *imageRGB, int height, int width)
{
    int i,j;
    FILE *file;
    file = fopen(output_file, "wb");

    fprintf(file, "P6\n");
    fprintf(file, "%d %d\n", width, height);
    fprintf(file, "%d\n", NUM_COLORS - 1);

    for (i = 0; i < height; i++)
    {
        for (j = 0; j < width; j++)
        {
            fwrite(imageRGB->pixels + i*width + j, 1, 3, file);
        }
    }

    fclose(file);
}


void *mandelbrot(void *void_ptr)
{//ImageRGB *imageRGB,double x_min, double x_max, double y_min, double y_max, double resolution,int steps, int width, int height;
    Params *par = (Params*)void_ptr;

    double x, y, re, im, new_re, new_im;
    int i, j, currentStep;
    double x_min = par->x_min;

    double y_min = par->y_min;

    double resolution = par->resolution;
    int steps = par->steps;
    int width = par->width;
    int height = par->height;

    

    for (i = 0; i < height; i++)
    {
        y = y_min + i*resolution;

        for(j = 0; j < width; j++)
        {
            re = 0;
            im = 0;
            currentStep = 0;

            x = x_min + j*resolution;

            while( re*re + im*im < 4  && currentStep < steps)
            {

                new_re = re * re - im * im + x;
                new_im = 2 * re * im + y;
                re = new_re;
                im = new_im;
                currentStep ++;

            }

            unsigned char r;
            unsigned char g;
            unsigned char b;

            hsvtorgb(&r, &g, &b, currentStep % 256, 255, 255 * (currentStep < steps) );
            par->imageRGB->pixels[i*width + j].r = r;
            par->imageRGB->pixels[i*width + j].g = g;
            par->imageRGB->pixels[i*width + j].b = b;
            pthread_mutex_lock(&lock);
    finalImageRGB->pixels[i*width + j] = par->imageRGB->pixels[i*width + j];
    pthread_mutex_unlock(&lock);
        }
    }
   /* pthread_mutex_lock(&lock);
    memcpy ( finalImageRGB->pixels, par->imageRGB->pixels, sizeof(par->imageRGB->pixels) );
    pthread_mutex_unlock(&lock);*/
    return NULL;   
}


void julia(ImageRGB *imageRGB,double x_min, double x_max, double y_min, double y_max, double resolution, int steps, double cx, double cy, int width, int height)
{
    double x = x_min;
    double y = y_min;
    double re, im, new_re, new_im;
    int i, j, currentStep;

    for (i = 0; i < height; i++)
    {

        y = y_min + i*resolution;

        for(j = 0; j < width; j++)
        {
            x = x_min + j*resolution;
            re = x;
            im = y;
            currentStep = 0;
            while( re*re + im*im < 4  && currentStep < steps)
            {

                new_re = re * re - im * im + cx;
                new_im = 2 * re * im + cy;
                re = new_re;
                im = new_im;
                currentStep ++;

            }

            unsigned char r;
            unsigned char g;
            unsigned char b;

            hsvtorgb(&r, &g, &b, currentStep % 256, 255, 255 * (currentStep < steps) );
            imageRGB->pixels[i*width + j].r = r;
            imageRGB->pixels[i*width + j].g = g;
            imageRGB->pixels[i*width + j].b = b;
        }
    }
}


int main(int argc, char *argv[]) 
{
        char *input_file = argv[1];
        char *output_file = argv[2];
        int num_threads = atoi(argv[3]);

        pthread_t* threads = (pthread_t*) malloc (num_threads*sizeof(pthread_t));
        
        //ImageRGB* partialImageRGB = (ImageRGB*) malloc (sizeof(ImageRGB));
        finalImageRGB = (ImageRGB*) malloc (sizeof(ImageRGB));
        int i;
        int flag = 0;

        int set_type, steps, width, height;

        double x_min, x_max , y_min, y_max;
        double resolution,cx, cy;
        

        read_file(input_file, &set_type, &x_min, &x_max , &y_min, &y_max, &resolution, &steps, &cx, &cy);

        int initialHeight = floor( fabs( (y_max - y_min)/resolution) );
        width = floor( fabs( (x_max - x_min)/resolution) );
        height = floor( ((fabs( (y_max - y_min)/resolution))/num_threads) );

        finalImageRGB->width = width;
        finalImageRGB->height = initialHeight;
        finalImageRGB->pixels = (Pixel *) malloc(width * initialHeight * sizeof(Pixel));

        if( set_type == 0)
        {
            for(i = 1; i <= num_threads; i++) {

                ImageRGB *imageRGB = (ImageRGB *) malloc(sizeof(ImageRGB));
                imageRGB->width = width;
                imageRGB->height = height;
                imageRGB->pixels = (Pixel *) malloc(width * height * sizeof(Pixel));

                Params *params = (Params*) malloc (sizeof(Params));
                params->imageRGB = imageRGB;
                params->x_min = x_min;
                params->x_max = x_max;
                if(flag == 0) {
                    params->y_min = y_min;
                    params->y_max = height*i;
                    flag = 1;
                } else {
                    params->y_min = height*(i-1);
                    params->y_max = height*i;
                }
                
                params->resolution = resolution;
                params->steps = steps;
                params->width = width;
                params->height = height;
                params->threadId = i;
                int aux = i - 1;
                //printf("Thread %i incepe prelucrarea\n", params->threadId);
                if(pthread_create(&threads[aux], NULL, mandelbrot, &params)) {

                    fprintf(stderr, "Error creating thread\n");
                    return 1;
                }
                if(pthread_join(threads[aux], NULL)) {
                    fprintf(stderr, "Error joining thread\n");
                    return 2;
                }
                
            }
            //mutex pe imaginea finala si adaugat in ordine
        }
        else
        {
           // julia(imageRGB,x_min, x_max , y_min, y_max, resolution, steps, cx, cy, width, height);
        }

        /* wait for the second thread to finish */


        write_file(output_file, finalImageRGB, initialHeight, width);
   
        return 0;
   }
