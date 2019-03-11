#! /usr/bin/env python
# encoding: utf-8

from waflib import Options
from waflib.Configure import conf

def options(opt):
    print ("entering mongodb option")
    opt.add_option('--with-mongodb', type='string', default=None,
                   dest='with_mongodb', help='''Path to mongodb, e.g., /usr/local''')

@conf
def check_mongodb(self, *k, **kw):
    root = k and k[0] or kw.get('path', None) or Options.options.with_mongodb
    mandatory = kw.get('mandatory', True)
    var = kw.get('uselib_store', 'MONGODB')

    if root:
        print ("root exist: " , root)
        self.check_cxx(lib='mongocxx',
                       msg='Checking for mongocxx library',
                       define_name='HAVE_%s' % var,
                       uselib_store=var,
                       mandatory=mandatory,
                       cxxflags="-I%s/include" % root,
                       linkflags="-L%s/lib" % root)

        self.check_cxx(lib='bsoncxx',
                       msg='Checking for bsoncxx library',
                       define_name='HAVE_%s' % var,
                       uselib_store=var,
                       mandatory=mandatory,
                       cxxflags="-I%s/include" % root,
                       linkflags="-L%s/lib" % root)

    else:
        try:
            self.check_cfg(package='libmongocxx',
                           args=['--cflags', '--libs'],
                           uselib_store='MONGODB',
                           mandatory=True)
        except:
            print ("CATCH!!!: ", var)
            self.check_cxx(lib='mongocxx',
                       msg='Checking for mongocxx library',
                       define_name='HAVE_MONGODB' ,
                       uselib_store='MONGODB',
                       mandatory=True,
                       cxxflags="-I/usr/local/include/mongocxx/v_noabi -I/usr/local/include/libmongoc-1.0   -I/usr/local/include/bsoncxx/v_noabi -I/usr/local/include/libbson-1.0",
                       linkflags="-L/usr/local/lib")

            self.check_cxx(lib='bsoncxx',
                       msg='Checking for bsoncxx library',
                       define_name='HAVE_MONGODB' ,
                       uselib_store='MONGODB',
                       mandatory=True,
                       cxxflags="-I/usr/local/include/mongocxx/v_noabi -I/usr/local/include/libmongoc-1.0   -I/usr/local/include/bsoncxx/v_noabi -I/usr/local/include/libbson-1.0",
                       linkflags="-L/usr/local/lib")

