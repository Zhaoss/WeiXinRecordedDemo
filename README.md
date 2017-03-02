# WeiXinRecordedDemo

[简书链接](http://www.jianshu.com/p/15f6efd66f83)

```
功能主要包含5点: 

1.基于ffmpeg的视频拍摄及合成;

2.自定义拍摄按钮, 长按放大并且显示拍摄进度;

3.自定义view, 实现手绘涂鸦;

4.自定义可触摸旋转缩放位移的表情文字view;

5.基于ffmpeg的图片和视频合成处理.
```

效果如图:

![image](http://p1.bpimg.com/1949/91265a1c314bbbcb.gif)

界面风格高仿微信, 只不过微信的编辑处理是作用于图片, 而我们的是基于视频, 所以如果你有需求, 把视频编辑处理换成图片编辑, 更是简单.

##1.实现使用ffmpeg录制视频
![image](http://upload-images.jianshu.io/upload_images/2582948-d1fa96643d11b381.png?imageMogr2/auto-orient/strip)

首先导入lib库和ffmpeg的录制java文件, 我使用的是第三方VCamera封装的ffmpeg, 他没有jar包, 所以需要将con.yixia包下的所有文件都copy过来,

![image](http://upload-images.jianshu.io/upload_images/2582948-92323efee5b4ca92.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

然后在application里面初始化VCamera:

![image](http://upload-images.jianshu.io/upload_images/2582948-c0d7cd1195f77d29.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
![image](http://upload-images.jianshu.io/upload_images/2582948-614e921e364ae44b.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这个时候, 你就可以在SurfaceView上看见拍摄预览界面了, 

然后mMediaRecorder.startRecord()拍摄视频, 

调用mMediaRecorder.stopRecord()停止录制视频, 

因为拍摄出来的文件是ts视频流, 所以还要调用mMediaRecorder.startEncoding()开始合成MP4视频文件.

MediaRecorderBase类还可以设置视频各个参数, 如:
![image](http://upload-images.jianshu.io/upload_images/2582948-c32e062be8e6967c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

##2.自定义拍摄按钮, 长按放大并且显示拍摄进度
![image](http://upload-images.jianshu.io/upload_images/2582948-ea056dedb3424b49.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

自定义RecordedButton继承View, 在onDraw里分三部分绘制:
![image](http://upload-images.jianshu.io/upload_images/2582948-f9da153e728525e4.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在拍摄模式下, 改变radius(半径), 达到放大或者缩小外圈和内圈圆的效果, 不断增加girth值达到显示拍摄进度的效果, 是不是很简单.

##3.自定义view, 实现手绘涂鸦

![image](http://upload-images.jianshu.io/upload_images/2582948-bb858f472413c026.png?imageMogr2/auto-orient/strip)

自定义TuyaView继承View, 重写onTouch(), 在手指点下和移动时实时绘制触摸轨迹:

![image](http://upload-images.jianshu.io/upload_images/2582948-d20beb371e9bacca.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

在手指按下时创建new Path()对象, 记录本次手指触摸移动轨迹, 并且实时调用invalidate() 达到不断调用onDraw()的目的, 然后使用canvas.drawPath(path,paint)绘制触摸路径, 是不是非常简单.

##4.自定义可触摸旋转缩放位移的表情文字view

![image](http://upload-images.jianshu.io/upload_images/2582948-34296a81d7d95621.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这个view稍微有点麻烦, 但我单独写了一篇文章[点击跳转](http://www.jianshu.com/p/15f6efd66f83), 非常详细的讲解了这个view, 而且封装的非常好, 只要addView到布局中就可以使用了, 大家可以点击链接过去看一下.

##5.基于ffmpeg的图片和视频合成处理

这也是demo的最后一步, 将涂鸦,和表情文字全部合成到视频当中, 首先是得到需要合成的图片, 我们可以通过view.draw(Canvas canvas),得到布局的bitmap:

![image](http://upload-images.jianshu.io/upload_images/2582948-bc672756893b8e29.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

然后通过ffmpeg来执行图片和视频的合成, 具体语句是这样的:

ffmpeg -i videoPath -i imagePath -filter_complex overlay=0:0 -vcodec libx264 -profile:v baseline -preset ultrafast -b:v 3000k -g 25 -f mp4 outPath

我把参数讲解一下: videoPath代表你要编辑视频的路径

imagePath代表你要合成的图片路径

outPath是合成之后的输出视频路径

这些是我们需要替换的参数至于一些别的, 例如:

overlay=0:0表示图片坐标位置, 0:0表示x轴=0,y轴=0

-vcodec后面表示视频输出格式, 3000k码率, 25帧数, 总之ffmpeg的参数还有很多, 如果感兴趣可以去ffmpeg官网看命令大全.

![image](http://upload-images.jianshu.io/upload_images/2582948-4896200dba364093.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

向UtilityAdapter.FFmpegRun()里传入ffmpeg语句就可以执行了, 返回值 int , 如果等于0就是成功, 非0则是失败, FFmpegRun()方法的第一参数如果传入空字符串就是异步执行视频处理, 否则就是同步执行, 这点要注意.

#如果这篇文章对大家有所帮助, 希望可以点一下star哦, 我会经常在上面分享我工作中遇到的问题和酷炫的特效实现.
