from django.db import models
import datetime
from fieldlogs.settings import *

class Log(models.Model):
    created = models.DateTimeField(auto_now_add = True)
    updated = models.DateTimeField(auto_now = True)

    latitude = models.FloatField()
    longitude = models.FloatField()
    location = models.CharField( max_length = 100, null = True, blank = True, help_text = 'Name of Location if any' )

    subject = models.CharField( max_length = 100 )
    comment = models.TextField( null = True, blank = True )

    nationality = models.CharField( max_length = 50, choices = LOG_NATIONALITIES )
    birthdate = models.DateField( verbose_name = 'Date of Birth' )

    barcode = models.TextField( null = True, blank = True, verbose_name = 'Raw BarCode Value', help_text = 'Raw Scanned Bar-Code Value' )

    def photo_upload_to(instance,filename):
        return "%s/%s/%s" % ('photo',instance.id,filename)
    photo = models.ImageField(upload_to = photo_upload_to, null=True, blank=True, help_text = 'Photo affiliated to this Log')
    photo_caption = models.CharField( max_length = 100, null=True, blank=True)

    def signature_upload_to(instance,filename):
        return "%s/%s/%s" % ('signature',instance.id,filename)
    signature = models.ImageField(upload_to = signature_upload_to, help_text = 'Signature of Data Entrant')
    def age(self):
        return (datetime.date.today() - self.birthdate).days/365
    def view_age(self):
        return '%s Years Old' % self.age()
    view_age.short_description = 'Age'
    def view_location(self):
            #return '''<a href="http://maps.google.com/maps/api/staticmap?center=%(lat)s,%(lng)s&zoom=13&markers=color:black|%(lat)s,%(lng)s&size=800x600&sensor=true" target="_blank">
            return '''<a href="http://maps.google.com/maps/?q=%(lat)s,%(lng)s" target="_blank">
            <img src="%(icon_uri)s" />
        </a>''' % {'lat':self.latitude, 'lng':self.longitude, 'icon_uri' : '%s/images/map.png' % STATIC_URL}
    view_location.short_description = 'View Location'
    view_location.allow_tags= True
    def view_photo(self):
        try:
            return '<img src="%s" height="100px" />' % (self.photo.url)
        except:
            return '<img src="%s" height="100px" />' % (DEFAULT_PHOTO_URI)
    view_photo.short_description = 'Affiliated Photo'
    view_photo.allow_tags= True
    def view_signature(self):
        try:
            return '<img src="%s" height="100px" title="%s" />' % (self.signature.url, self.photo_caption)
        except:
            return '<img src="%s" height="100px" />' % (DEFAULT_SIG_URI)
    view_signature.short_description = 'Affiliated Signature'
    view_signature.allow_tags= True

    def to_dict(self):
        photo_uri = ''
        try:
            photo_uri = self.photo.url
        except:
            pass

        sig_uri = ''
        try:
            sig_uri = self.signature.url
        except:
            pass

        return {
                'id' : self.id,
                'latitude' : self.latitude,
                'longitude' : self.longitude,
                'location' : self.location,
                'subject' : self.subject,
                'comment' : self.comment,
                'nationality' : self.get_nationality_display(),
                'birthdate' : self.birthdate.strftime('%Y-%m-%d'),
                'barcode' : self.barcode,
                'photo' : photo_uri,
                'photo_caption' : self.photo_caption,
                'signature' : sig_uri
                }

    class Meta:
        db_table = 'logs'
        verbose_name = 'Field Log'
        verbose_name_plural = 'Field Logs'

