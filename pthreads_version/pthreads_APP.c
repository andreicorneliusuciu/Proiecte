    #include <stdio.h>
    #include <math.h>
    #include <malloc.h>
    #include <unistd.h>
    #include <string.h>
    #include <pthread.h>
    #define NUM_COLORS 256


#include <stdint.h>

typedef struct
{
    unsigned char r;
    unsigned char g;
    unsigned char b;
} Pixel;

typedef struct 
{
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

void write_file(FILE *file, ImageRGB *imageRGB, int height, int width)
{
    int i,j;

    for (i = 0; i < height; i++)
    {
        for (j = 0; j < width; j++)
        {
            fwrite(imageRGB->pixels + i*width + j, 1, 3, file);
        }
    }
}


void *mandelbrot(void *void_ptr)
{
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
        }
    }

    return NULL;   
}

void *julia(void *void_ptr)
{
    Params *par = (Params*)void_ptr;

    double re, im, new_re, new_im;
    int i, j, currentStep;
    double x_min = par->x_min;
    double cx = par->cx;
    double cy = par->cy;
    double x = par->x_min;
    double y = par->y_min;
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
            par->imageRGB->pixels[i*width + j].r = r;
            par->imageRGB->pixels[i*width + j].g = g;
            par->imageRGB->pixels[i*width + j].b = b;
        }
    }

    return NULL;   
}

int main(int argc, char *argv[]) 
{
        char *input_file = argv[1];
        char *output_file = argv[2];
        char *th = argv[3];
        int num_threads = atoi(th);
        int i, aux, flag = 0;

        int set_type, steps, width, height;

        double x_min, x_max , y_min, y_max;
        double resolution,cx, cy;

        read_file(input_file, &set_type, &x_min, &x_max , &y_min, &y_max, &resolution, &steps, &cx, &cy);

        //Se calculeaza width,height si chunk
        width = floor( fabs( (x_max - x_min)/resolution) );
        int initialHeight = floor( fabs( (y_max - y_min)/resolution));
        height = floor( (fabs( (y_max - y_min)/resolution)/num_threads) );

        //Vectorul cu threaduri
        pthread_t *threads = (pthread_t*) malloc (num_threads*sizeof(pthread_t));

        //vector de parametrii care contine fiecare imagine partiala
        Params *paramsArray = (Params*) malloc (num_threads*sizeof(Params));

        pthread_attr_t attr;
        pthread_attr_init(&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

        if( set_type == 0)
        {
            for(i = 1; i <= num_threads; i++){
                aux = i-1;
      
                paramsArray[aux].imageRGB = (ImageRGB *) malloc(sizeof(ImageRGB));
                paramsArray[aux].imageRGB->pixels = (Pixel *) malloc(width * height * sizeof(Pixel));
                paramsArray[aux].x_min = x_min;
                paramsArray[aux].x_max = x_max;

                if(flag == 0) {
                    paramsArray[aux].y_min = y_min;
                    paramsArray[aux].y_max = y_min + (height*i);
                    printf("Thread %d face de la %0.12lf pana la %0.12lf\n", i, y_min, y_max);
                    flag = 1;
                } else {
                    paramsArray[aux].y_min = y_min + (height*aux)*resolution;
                    paramsArray[aux].y_max = y_min + height*i*resolution;
                    printf("Thread %d face de la %0.12lf pana la %0.12lf\n", i, paramsArray[aux].y_min, paramsArray[aux].y_max);
                }

                paramsArray[aux].resolution = resolution;
                paramsArray[aux].steps = steps;
                paramsArray[aux].width = width;
                paramsArray[aux].height = height;

                if(pthread_create(&threads[aux], &attr, mandelbrot, (void *)&paramsArray[aux])) {

                    fprintf(stderr, "Error creating thread\n");
                    return 1;
                }
            }   
        }
        else
        {
            for(i = 1; i <= num_threads; i++){
                aux = i-1;

                paramsArray[aux].imageRGB = (ImageRGB *) malloc(sizeof(ImageRGB));
                paramsArray[aux].imageRGB->pixels = (Pixel *) malloc(width * height * sizeof(Pixel));
                paramsArray[aux].x_min = x_min;
                paramsArray[aux].x_max = x_max;
                paramsArray[aux].cx = cx;
                paramsArray[aux].cy = cy;

                if(flag == 0) {
                    paramsArray[aux].y_min = y_min;
                    paramsArray[aux].y_max = y_min + (height*i);
                    printf("Thread %d face de la %0.12lf pana la %0.12lf\n", i, y_min, y_max);
                    flag = 1;
                } else {
                    paramsArray[aux].y_min = y_min + (height*aux)*resolution;
                    paramsArray[aux].y_max = y_min + height*i*resolution;
                    printf("Thread %d face de la %0.12lf pana la %0.12lf\n", i, paramsArray[aux].y_min, paramsArray[aux].y_max);
                }

                paramsArray[aux].resolution = resolution;
                paramsArray[aux].steps = steps;
                paramsArray[aux].width = width;
                paramsArray[aux].height = height;

                if(pthread_create(&threads[aux], &attr, julia, (void *)&paramsArray[aux])) {

                    fprintf(stderr, "Error creating thread\n");
                    return 1;
                }
            }
        }

        /*Fac join la toate threadurile*/
        for(i = 0; i < num_threads; i++) {
            if(pthread_join(threads[i], NULL)) {
                    fprintf(stderr, "Error joining thread\n");
                    return 2;
                }
        }

        FILE *file;
        file = fopen(output_file, "a+");

        fprintf(file, "P6\n");
        fprintf(file, "%d %d\n", width, initialHeight);
        fprintf(file, "%d\n", NUM_COLORS - 1);

        /*Scriu rez final*/
        for(i = 0; i < num_threads; i++) {
            write_file(file, paramsArray[i].imageRGB, paramsArray[i].height, paramsArray[i].width);
        }
        
        fclose(file);
        return 0;
   }
