## v2.5 增加剪切时长功能, 优化交互
## v2.4 大幅优化, 使用前后摄像头拍摄视频时, 合成视频过慢的问题 (感谢@bertsir)
## v2.3 增加摄像头切换和闪光灯开启功能, 修复前置摄像头拍摄出来画面颠倒等bug
<br /> 

```
功能概括: 

1.基于ffmpeg的视频分段拍摄及合成;

2.自定义拍摄按钮, 显示多段视频拍摄进度, 和删除视频段落模式;

3.自定义view, 实现手绘涂鸦;

4.自定义可触摸旋转缩放位移的表情文字view;

5.仿微信裁剪图片控件, 自定义View实现功能;

6.基于ffmpeg改变视频速度(加速或者减速);

7.基于ffmpeg裁剪视频宽高;

8.基于ffmpeg的图片和视频合成处理.
```

# 如果想了解实现思路, 这里有详细源码分析讲解

<a href="http://www.jianshu.com/p/5a173841a828" target="_blank">仿微信视频拍摄UI, 基于ffmpeg的视频录制编辑(上)</a>

<a href="http://www.jianshu.com/p/df568b7141c5" target="_blank">仿微信视频拍摄UI, 基于ffmpeg的视频录制编辑(下)</a>

![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo1.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo4.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo5.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo6.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo3.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo7.png?raw=true)
![image](https://github.com/Zhaoss/WeiXinRecordedDemo/blob/master/%E8%BF%99%E6%98%AF%E7%AE%80%E4%B9%A6%E4%B8%8A%E7%AF%87%E7%9A%84%E6%BA%90%E7%A0%81,%E5%B8%AE%E5%8A%A9%E7%90%86%E8%A7%A3%E6%BA%90%E7%A0%81%E5%AE%9E%E7%8E%B0/Image/demo8.png?raw=true)

# 如果这篇文章对大家有所帮助, 希望可以点一下star哦, 我会经常在上面分享我工作中遇到的问题和酷炫的特效实现.

## 本项目所使用的so库是VCamera，个人免费， 禁止商用，只用作demo演示
