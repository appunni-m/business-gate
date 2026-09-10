#!/usr/bin/env python3
"""Check the declared Business Gate text/surface pairs; this is not a TalkBack or native-control audit."""
from pathlib import Path
import xml.etree.ElementTree as ET


def luminance(color):
    rgb = [int(color[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [c / 12.92 if c <= .04045 else ((c + .055) / 1.055) ** 2.4 for c in rgb]
    return sum(c * weight for c, weight in zip(linear, (.2126, .7152, .0722)))


pairs = [('ink', surface) for surface in ('background', 'surface', 'accent_surface', 'amber_surface')]
pairs += [('muted', surface) for surface in ('background', 'surface', 'accent_surface')]
pairs += [('accent', surface) for surface in ('background', 'surface', 'accent_surface')]
pairs += [('on_accent', 'accent'), ('amber', 'amber_surface'), ('danger', 'surface'), ('danger', 'background')]
checks = 0
lowest = 100.0
for theme in ('values', 'values-night'):
    colors = {node.attrib['name']: node.text.strip() for node in ET.parse(Path('app/src/main/res') / theme / 'colors.xml').getroot()}
    for foreground, background in pairs:
        levels = sorted((luminance(colors[foreground]), luminance(colors[background])))
        ratio = (levels[1] + .05) / (levels[0] + .05)
        assert ratio >= 4.5, f'{theme} {foreground}/{background}: {ratio:.2f}:1 is below 4.5:1'
        checks += 1
        lowest = min(lowest, ratio)
print(f'PASS {checks} declared light/dark text contrast pairs; minimum {lowest:.2f}:1')
focus_checks = 0
for theme in ('values', 'values-night'):
    colors = {node.attrib['name']: node.text.strip() for node in ET.parse(Path('app/src/main/res') / theme / 'colors.xml').getroot()}
    for foreground, background in [('focus', surface) for surface in ('background', 'surface', 'accent_surface', 'amber_surface')] + [('on_accent', 'accent')]:
        levels = sorted((luminance(colors[foreground]), luminance(colors[background])))
        ratio = (levels[1] + .05) / (levels[0] + .05)
        assert ratio >= 3, f'{theme} focus {foreground}/{background}: {ratio:.2f}:1 is below 3:1'
        focus_checks += 1
print(f'PASS {focus_checks} declared focus indicator pairs; rendered indicators still require device checks')
