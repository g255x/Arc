/*
 * Original shader from: https://www.shadertoy.com/view/fddXWH
 */

#ifdef GL_ES
precision highp float;
#endif

// glslsandbox uniforms
uniform float time;
uniform vec2 resolution;

// shadertoy emulation
#define iTime time
#define iResolution resolution

// --------[ Original ShaderToy begins here ]---------- //
mat2 rotate2D(float r){
    return mat2(cos(r), sin(r), -sin(r), cos(r));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    float i,e=0.,f=0.,s,g,k=.01;    
    float o=0.;
    o++;    
    for(int i=0;i<100;i++)
    {
      s=2.;      
      g+=min(f,max(.03,e))*.3;      
      vec3 p=vec3((fragCoord.xy-iResolution.xy/s)/iResolution.y*g,g-2.);
      p.yz*=rotate2D(-.8);
      p.z+=iTime;
      e=p.y;
      f=p.y;
      for(int x=0;x<10;++x)
      {
        if (s>=200.) break;
        s/=.6;
        p.xz*=rotate2D(s);
        e+=abs(dot(sin(p*s)/s,p-p+.4));
        f+=abs(dot(sin(p.xz*s*.6)/s,iResolution.xy/iResolution.xy));
      }

      if(f>k*k)
        o+=e*o*k;
      else
        o+=-exp(-f*f)*o*k;


    }
    fragColor = vec4(o);
}
// --------[ Original ShaderToy ends here ]---------- //

void main(void)
{
    mainImage(gl_FragColor, gl_FragCoord.xy);
}