#! /usr/bin/env python
# encoding: utf-8

from waflib import Options
from waflib.Configure import conf

def options(opt):
    print ("entering zlib  option")    
    opt.add_option('--with-zlib', type='string', default=None,
                   dest='with_zlib', help='''Path to zlib, e.g., /usr/local''')

@conf
def check_zlib(self, *k, **kw):
    print ("entering check_zlib")
    root = k and k[0] or kw.get('path', None) or Options.options.with_zlib
    mandatory = kw.get('mandatory', True)
    var = kw.get('uselib_store', 'ZLIB')

    if root:
        print ("root exist: " , root)
        self.check_cxx(lib='libz',
                       msg='Checking for zlib library',
                       define_name='HAVE_%s' % var,
                       uselib_store=var,
                       mandatory=mandatory,
                       cxxflags="-I%s/include" % root,
                       linkflags="-L%s/lib" % root)
    else:
        try:
            print ("root not exist: ")
            self.check_cfg(package='zlib',
                           args=['--libs'],
                           uselib_store='ZLIB',
                           mandatory=True)
        except:
            print ("CATCH!!!: ", var)
            self.check_cxx(
                       msg='Checking for zlib library',
                       define_name='HAVE_ZLIB' ,
                       uselib_store='ZLIB',
                       mandatory=True,
                       linkflags="-lz")
