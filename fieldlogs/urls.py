from django.conf.urls import patterns, include, url
from django.views.generic.simple import redirect_to
from fieldlogs.settings import BASE_URL

from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    url(r'^admin/', include(admin.site.urls)),
)

urlpatterns += patterns('',
    (r'^$', redirect_to, {'url': '%s/admin/' % BASE_URL}),
)

urlpatterns += patterns('',
    (r'^grappelli/', include('grappelli.urls')),
)